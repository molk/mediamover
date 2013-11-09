import static CpUtil.*

/**
 * Groovy script setting your photos and movies captured in an iPhoto library free.
 *
 * Travels the directory tree starting at sourceRoot and copies every file to targetRoot,
 * using the {@link File#lastModified() file date} and type (file suffix) to determine
 * the target sub directory:
 *
 * $targetRoot/{fotos|videos}/$file_year/$file_month
 *
 * Duplicate files are handled by simply appending '_n' with n being the number
 * resulting in a filename unique for the given month sub directory.
 *
 * @author Marcus Olk
 *
 * 5.7.2013
 */

final sourceRoot = '/Volumes/Leopard/Users/marcus/Pictures/iPhoto Library/Originals'
final targetRoot = '/Volumes/toshi_1tb/iPhoto_Export'

final line = '-' * 100

final long startTimestamp = System.currentTimeMillis()

long filesCopied = 0
long bytesCopied = 0

println line

(sourceRoot as File).eachFileRecurse(groovy.io.FileType.FILES) { final File srcFile ->
    final lastModified = new Date(srcFile.lastModified())

    final mediaFolder = isVideo(srcFile) ? 'videos' : 'fotos'

    final year  = lastModified[Calendar.YEAR] as String
    final month = ((lastModified[Calendar.MONTH] + 1) as String).padLeft(2, '0')

    final destFile = new File("$targetRoot/$mediaFolder/$year/$month", srcFile.name)

    copy srcFile, destFile

    println line

    filesCopied += 1
    bytesCopied += srcFile.size()
}

final long durationMillis = System.currentTimeMillis() - startTimestamp

println "$filesCopied files in ${formatWithUnits(bytesCopied)}"
println "Took ${timeAsString(durationMillis)}"

//~ utility functions requiring class scope

class CpUtil {

    static copy(File src, File dest) {

        createParentDirectoryIfNeccessary dest

        if (dest.exists()) {
            println "$dest already exists. Creating alternative name ..."
            int i = 0
            while (dest.exists()) {
                i += 1
                dest = new File(dest.parent, dest.name + "_$i")
            }
            println "Using $dest.name"
        }

        // Groovy way of copying binary file contents
        // but to memory intensive?
        // dest.bytes = src.bytes

        src.withInputStream { inStream ->
            dest.withOutputStream { outStream ->
                outStream << inStream
            }
        }

        // keep the original file date
        dest.lastModified = src.lastModified()

        println src
        println "-> $dest"
        println "${formatWithUnits(src.size())} copied (${formatTimestamp(src.lastModified())})"
    }

    static createParentDirectoryIfNeccessary(File file) {
        File dir = file.isDirectory() ?: file.parentFile

        if (!dir.exists()) {
            if (!dir.mkdirs()) {
                throw new IOException("Failed to create $dir")
            }
        }
    }

    static final videoSuffixes = ['mov', 'avi', 'mp4', 'm4v', 'mpg', 'mpeg'].asImmutable()

    static boolean isVideo(File file) {
        final suffix = file.name.toLowerCase().tokenize('.').last()
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
