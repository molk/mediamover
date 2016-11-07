import groovy.transform.ToString

import java.nio.file.Files
import java.nio.file.attribute.BasicFileAttributeView
import java.nio.file.attribute.BasicFileAttributes
import java.nio.file.attribute.FileTime
import java.util.concurrent.TimeUnit

import static MediaFile.assertIsExistingDirectory
import static MediaFile.copy
import static MediaFile.formatWithUnits
import static MediaFile.isVideo
import static MediaFile.timeAsString

/**
 * Copies fotos and movies from a source directory into a media archive directory.
 *
 * Copies every file to targetRoot using the {@link File#lastModified() file date} and type (file suffix)
 * to determine the target sub directory:
 *
 * $targetRoot/{fotos|videos}/$file_year/$file_month
 *
 * Duplicate files are handled by simply appending '_n' with n being the number
 * resulting in a filename unique for the given month sub directory.
 *
 * @author Marcus Olk
 *
 * 04.11.2016
 */

keepSourceFiles = false // copy or move
dryRun = false
logfile = 'default'

sourceDir      = assertIsExistingDirectory '/Volumes/fotosundso/unsortiert/android'
destinationDir = assertIsExistingDirectory '/Volumes/fotosundso/t'
fileFilter     = new MediaFile.Filter('all')
logger         = new Logger(logfile)

// --------------------------------------------------------------------------------------------------------------------------------

startTimestamp = System.currentTimeMillis()

mediaFiles = sourceDir.listFiles(fileFilter)

line = '-' * 200

logger.log line

logger.log "Started : ${new Date(startTimestamp).format 'dd.MM.yyyy HH:mm:ss'}"
logger.log "From    : $sourceDir"
logger.log "To      : $destinationDir"
logger.log "Filter  : $fileFilter"
logger.log "Files   : ${mediaFiles.size()}"
logger.log "Mode    : ${keepSourceFiles ? 'copy' : 'move'} ${!dryRun ? '' : '(dry run)'}"

logger.log line

long sumOfFilesCopied = 0
long sumOfBytesCopied = 0
def  messages = []

mediaFiles.each { srcFile ->

    def result = \
        copy srcFile: srcFile, destFile: destFileFor(srcFile), \
             keepSource: keepSourceFiles, dryRun: dryRun, logger: logger

    if (result.bytesCopied) {
        sumOfFilesCopied += 1
        sumOfBytesCopied += result.bytesCopied
    }

    if (result.messages) {
        messages.addAll result.messages
    }

    logger.log line
}

def durationMillis = System.currentTimeMillis() - startTimestamp

if (messages) {
    logger.log "\n$line"
    logger.log "The following files were not moved:"
    messages.sort { it.contains '/fotos/' } .each { logger.log it }
    logger.log "$line\n"
}

if (!sumOfFilesCopied) {
    logger.log 'No files pushed.'
}
else {
    logger.log "$sumOfFilesCopied files in ${formatWithUnits(sumOfBytesCopied)}"
}


println "Took ${timeAsString(durationMillis)}"


// helpers --------------------------------------------------------------------------

File destFileFor(File srcFile) {
    def lastModified = new Date(FileInfo.fromFile(srcFile).created)

    def mediaFolder = isVideo(srcFile) ? 'videos' : 'fotos'
    def year        = lastModified[Calendar.YEAR].toString()
    def month       = (lastModified[Calendar.MONTH] + 1).toString().padLeft(2, '0')
    def destFolder  = "${destinationDir.absolutePath}/$mediaFolder/$year/$month"

    new File(destFolder, srcFile.name)
}

// -----------------------------------------------------------------------------------------------------------------------------------

class Logger {

    File logfile

    Logger(String logfilePath) {
        logfile = createIfNotExists logfilePath
    }

    def log(String message) {
        println message

        if (logfile) logfile.append "$message\n"
    }

    private static createIfNotExists(String logfilePath) {
        if (!logfilePath)  null
        else {
            def logfile = (logfilePath == 'default') \
                ? new File(System.properties.'user.dir' as String, "mm-${new Date().format('yyyyMMdd-HHmmss')}.log")
                : new File(logfilePath)

            if (!logfile.exists()) {
                if (!logfile.parentFile.exists()) {
                    assert logfile.parentFile.mkdirs(), "Failed to create logfile parent folder $logfile.parentFile"
                }
                assert logfile.createNewFile(), "Failed to create logfile $logfile"
            }

            logfile
        }
    }

}

// -----------------------------------------------------------------------------------------------------------------------------------

@ToString
class FileInfo {

    String path
    long size
    Long lastModified
    Long lastAccess
    Long created

    long getCreated() {
        Long value = created ?: lastModified

        assert value, 'file creation date must not be missing here'

        value
    }

    static FileInfo fromFile(file) {
        file = file as File

        def toMillis = { it?.to TimeUnit.MILLISECONDS }

        def attributes = Files.readAttributes file.toPath(), BasicFileAttributes

        new FileInfo(
            path: file.absolutePath,
            size: attributes.size(),
            created: toMillis (attributes.creationTime()),
            lastAccess: toMillis (attributes.lastAccessTime()),
            lastModified: toMillis (attributes.lastModifiedTime()))
    }

    File rightShift(File file) {
        def fromMillis = { Long ms -> ms ? FileTime.fromMillis(ms) : null }

        Files.getFileAttributeView(file.toPath(), BasicFileAttributeView)
            .setTimes(
                fromMillis(lastModified),
                fromMillis(lastAccess),
                fromMillis(created))

        file
    }

    String getDates() {
        def s = { Long ms -> !ms ? 'n.a.' : new Date(ms).format('yyyy-MM-dd HH:mm:ss') }

        "created: ${s created}, last modified: ${s lastModified}, last access: ${s lastAccess}"
    }
}

// -----------------------------------------------------------------------------------------------------------------------------------

class MediaFile {

    static class Filter implements FileFilter {

        enum MediaType {
            Images, Videos, All
        }

        private MediaType mediaType

        Filter(String mediaTypeParam) {
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
            if (pathname.isDirectory() || pathname.name.startsWith('.')) return false

            switch (mediaType) {
                case MediaType.Images: return !isVideo(pathname)
                case MediaType.Videos: return isVideo(pathname)
                default: return true
            }
        }

    }

    static File assertIsExistingDirectory(dir) {
        final File directory = dir as File

        if (!directory.exists() || !directory.isDirectory()) {
            println "$dir not a directory"
            System.exit(-1)
        }

        directory
    }

    static Map copy(Map params) {

        File srcFile = params.srcFile
        File destFile = params.destFile

        Logger logger = params.logger

        boolean keepSource = params.keepSource == null ? true : params.keepSource
        boolean dryRun     = params.dryRun == null ? false : params.dryRun

        assert srcFile, "src file missing"
        assert destFile, "dest file missing"
        assert logger, "logger missing"

        assert srcFile.isFile()
        assert srcFile.exists()

        if (!dryRun) createParentDirectoryIfNeccessary destFile

        if (destFile.exists()) {
            if (srcFile.size() != destFile.size()) {
                while (destFile.exists() && srcFile.size() != destFile.size()) {
                    destFile = new File(destFile.parent, appendVersionTo(destFile.name))
                }
            }

            if (srcFile.size() == destFile.size()) {
                def messages = ["$srcFile.name: Skipped as existing destination file has the same size as source: $destFile"]

                def srcFileInfo = FileInfo.fromFile srcFile
                def destFileInfo = FileInfo.fromFile destFile

                if (srcFileInfo.created > destFileInfo.created) {
                    messages << "Destination file dates set to source file dates: ${srcFileInfo.dates} (was ${srcFileInfo.dates})"
                    srcFileInfo >> destFile
                }

                messages.each { logger.log it }

                return [bytesCopied: 0, messages: messages]
            }

            logger.log "Using ${destFile.name}."
        }

        def srcFileInfo = FileInfo.fromFile srcFile

        logger.log "${keepSource ? 'Copy' : 'Move'} $srcFile.name -> $destFile"
        logger.log "${formatWithUnits(srcFileInfo.size)}, $srcFileInfo.dates"

        if (!dryRun) {

            if (!keepSource) {
                Files.move srcFile.toPath(), destFile.toPath()
            }
            else {
                // copy
                srcFile.withInputStream { inStream ->
                    destFile.withOutputStream { outStream ->
                        outStream << inStream
                    }
                }
            }

            def destFileInfo = FileInfo.fromFile destFile

            if (destFileInfo.created != srcFileInfo.created) {
                srcFileInfo >> destFile
                destFileInfo = FileInfo.fromFile destFile
            }

            assert destFileInfo.created == srcFileInfo.created
        }

        [bytesCopied: (destFile.exists() ? destFile : srcFile).size()]
    }

    static createParentDirectoryIfNeccessary(File file) {
        File dir = file.isDirectory() ?: file.parentFile

        if (!dir.exists()) {
            if (!dir.mkdirs()) {
                throw new IOException("Failed to create $dir")
            }
        }
    }

    static String appendVersionTo(String name) {
        if (name == null) return null
        if (name.isEmpty()) return ""

        final suffix = suffixOf(name)

        if (suffix) {
            name = name - suffix
        }

        final int iVersion = name.lastIndexOf("_v")

        if (iVersion < 0 || iVersion == name.length() - 1) {
            name = name + "_v1"
        }
        else {
            final int currentVersion = Integer.parseInt(name.substring(iVersion + 2))

            name = name.substring(0, iVersion) + "_v" + (currentVersion + 1)
        }

        return name + suffix
    }

    static String suffixOf(final String name) {
        final int i = name.lastIndexOf(".")
        (i < 0 || i == name.length() - 1) ? '' : name.substring(i)
    }

    static final videoSuffixes = ['mov', 'avi', 'mp4', 'm4v', 'mpg', 'mpeg', '3gp'].asImmutable()

    static boolean isVideo(File file) {
        isVideo file.name
    }

    static boolean isVideo(String filename) {
        final suffix = filename.toLowerCase().tokenize('.').last()
        videoSuffixes.any { it == suffix }
    }

    static final int ONE_MEG = 1024 * 1024

    static String formatWithUnits(long bytes) {
        int u = 0

        for (; bytes > ONE_MEG; bytes >>= 10) u++
        if (bytes > 1024) u++

        String.format('%.2f %cB', bytes / 1024f, ' KMGTPE'.charAt(u))
    }

    private static final int MS_PER_DAY = 1000 * 60 * 60 * 24
    private static final int MS_PER_HOUR = 1000 * 60 * 60
    private static final int MS_PER_MIN = 1000 * 60
    private static final int MS_PER_SEC = 1000

    static String timeAsString(long milliSecs) {
        String s = ""

        final int days = (int) (milliSecs / MS_PER_DAY)
        final int hours = (int) (milliSecs % MS_PER_DAY) / MS_PER_HOUR
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
}
