#!/Users/marcus/Applications/groovy/2.0/bin/groovy

import static CpUtil.*

/**
 * Groovy script simply copying image files with finding unique target filenames if existing file is not the same.
 *
 * Duplicate files are handled by simply appending '_n' with n being the number
 * resulting in a filename unique.
 *
 * @author Marcus Olk
 *
 * 4.8.2013
 */

/* Optional CLI parsing

def cli = new CliBuilder (
    usage: 'safe_copy cliTest -s sdkdir {projectName}',
    header: 'Safely copying image or movie files finding unique target filenames if target file is not the same as source')

cli.h (longOpt: 'help', 'print usage information')
cli.t (longOpt: 'type', args: 1, 'type of media file (img or vid)')
cli.s (longOpt: 'source', args: 1, 'source directory to copy')
cli.d (longOpt: 'destination', args: 1, 'destination directory to copy')

def opt = cli.parse(args)

if (!opt || opt.h) {
    cli.usage()
    System.exit 1
}

if (!opt.s || !opt.d) {
    cli.usage()
    println '\nSource and destination directory are mandatory'
    System.exit 1
}

final File sourceDir        = exitIfIllegalDirectory(opt.s as File)
final File destinationDir   = exitIfIllegalDirectory(opt.d as File)

final fileFilter = new FileFilter(opt.t as String)
*/

final File sourceDir        = exitIfIllegalDirectory('/Volumes/Leopard/Users/marcus/Pictures/backup/07_2013')
final File destinationDir   = exitIfIllegalDirectory('/Volumes/toshi_1tb/Fotos_und_Videos/fotos/2013/07/')

final fileFilter = new MediaFileFilter('img')

long filesCopied = 0
long bytesCopied = 0

println "Copying: $fileFilter"
println "From   : $sourceDir"
println "To     : $destinationDir"

final filesToCopy = sourceDir.listFiles(fileFilter)

filesToCopy.each { final File srcFile ->
    if (copy(srcFile, destinationDir)) {
        filesCopied += 1
        bytesCopied += srcFile.size()
    }
}

println "$filesCopied files copied."

if (bytesCopied > 0) {
    println formatWithUnits(bytesCopied)
}

//~ utility functions requiring class scope

class MediaFileFilter implements FileFilter {

    enum MediaType { Images, Videos, All }

    private MediaType mediaType

    MediaFileFilter(String mediaTypeParam) {
        mediaTypeParam = mediaTypeParam?.toLowerCase()

        switch (mediaTypeParam) {
            case 'img': mediaType = MediaType.Images; break;
            case 'vid': mediaType = MediaType.Videos; break;
            default: mediaType = MediaType.All
        }
    }

    @Override
    String toString() { mediaType }

    @Override
    boolean accept(File pathname) {
        if (pathname.isDirectory()) return false

        switch (mediaType) {
            case MediaType.Images: return !isVideo(pathname)
            case MediaType.Videos: return isVideo(pathname)
            default: return true
        }
    }

}

class CpUtil {

    static File exitIfIllegalDirectory(dir) {
        final File directory = dir as File

        if (!directory.exists() || !directory.isDirectory()) {
            println "$dir not a directory"
            System.exit(-1)
        }

        return directory
    }

    static boolean copy(final File srcFile, File dest) {

        assert srcFile.isFile()
        assert srcFile.exists()

        assert dest.isDirectory()
        assert dest.exists()

        dest = new File(dest, srcFile.name)

        if (dest.exists()) {
            print "$srcFile.name already exists. "

            if (srcFile.size() != dest.size()) {
                while (dest.exists() && srcFile.size() != dest.size()) {
                    dest = new File(dest.parent, appendVersionTo(dest.name))
                }
            }

            if (srcFile.size() == dest.size()) {
                println "Skipped as destination $dest.name has the same size as source."
                return false
            }

            println "Using ${dest.name}."
        }

        print "$srcFile.name -> $dest.name "

        srcFile.withInputStream { inStream ->
            dest.withOutputStream { outStream ->
                outStream << inStream
            }
        }

        // keep the original file date
        dest.lastModified = srcFile.lastModified()

        println "(${formatWithUnits(srcFile.size())})"

        return true
    }

    static String appendVersionTo(String name) {
        if (name == null)   return null
        if (name.isEmpty()) return ""

        final suffix = suffixOf(name)

        if (suffix) {
            name = name - suffix
        }

        final int iVersion = name.lastIndexOf("_v")

        if (iVersion < 0 || iVersion == name.length()-1) {
            name = name + "_v1"
        }
        else {
            final int currentVersion = Integer.parseInt(name.substring(iVersion+2))

            name = name.substring(0, iVersion) + "_v" + (currentVersion+1)
        }

        return name + suffix
    }

    static String suffixOf(final String name) {
        final int i = name.lastIndexOf(".")
        (i < 0 || i == name.length()-1) ? '' : name.substring(i)
    }

    static final videoSuffixes = ['mov', 'avi', 'mp4', 'm4v', 'mpg', 'mpeg'].asImmutable()

    static boolean isVideo(File file) {
        isVideo file.name
    }

    static boolean isVideo(String filename) {
        final suffix = filename.toLowerCase().tokenize('.').last()
        videoSuffixes.any { it == suffix }
    }

    static final int ONE_MEG = 1024*1024

    static String formatWithUnits(long bytes) {
        int u = 0

        for (;bytes > ONE_MEG; bytes >>= 10) u++
        if (bytes > 1024) u++

        String.format('%.2f %cB', bytes/1024f, ' KMGTPE'.charAt(u))
    }

    private static final int MS_PER_DAY  = 1000 * 60 * 60 * 24
    private static final int MS_PER_HOUR = 1000 * 60 * 60
    private static final int MS_PER_MIN  = 1000 * 60
    private static final int MS_PER_SEC  = 1000

    static String timeAsString(long milliSecs) {
        String s = ""

        final int days    = (int) (milliSecs / MS_PER_DAY)
        final int hours   = (int) (milliSecs % MS_PER_DAY) / MS_PER_HOUR
        final int minutes = (int) (milliSecs % MS_PER_HOUR) / MS_PER_MIN
        final int seconds = (int) (milliSecs % MS_PER_MIN) / MS_PER_SEC

        if (days != 0)
            s += days + " day" + ((days > 1) ? "s" : "")

        if (hours != 0)
            s += (!s.isEmpty() ? ", " : "") + hours + " hour" + ((hours > 1) ? "s" : "")

        if (minutes != 0)
            s += (!s.isEmpty() ? ", " : "") + minutes + " minute" + ((minutes > 1) ? "s" : "")

        if (seconds != 0)
            s += (!s.isEmpty() ? ", " : "") + seconds + " second" + ((seconds > 1) ? "s" : "")

        !s.isEmpty() ? s : (milliSecs + " ms");
    }

    static final dateFormatter = new java.text.SimpleDateFormat('yyyy-MM-dd HH:mm:ss')

    static String formatTimestamp(long timestamp) {
        dateFormatter.format(new Date(timestamp))
    }
}
