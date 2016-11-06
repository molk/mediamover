package fotoarchiver

import java.nio.file.Files

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

        return directory
    }

    static long copy(Map params) {

        File srcFile = params.srcFile
        File destFile = params.destFile

        boolean keepSource = params.keepSource == null ? true : params.keepSource
        boolean dryRun     = params.dryRun == null ? false : params.dryRun

        assert srcFile, "src file missing"
        assert destFile, "dest file missing"

        assert srcFile.isFile()
        assert srcFile.exists()

        if (!dryRun) createParentDirectoryIfNeccessary destFile

        if (destFile.exists()) {
            print "$srcFile.name already exists. "

            if (srcFile.size() != destFile.size()) {
                while (destFile.exists() && srcFile.size() != destFile.size()) {
                    destFile = new File(destFile.parent, appendVersionTo(destFile.name))
                }
            }

            if (srcFile.size() == destFile.size()) {
                println "Skipped as destination $destFile.name has the same size as source."
                return 0
            }

            println "Using ${destFile.name}."
        }

        println "${keepSource ? 'Copy' : 'Move'} srcFile.name -> $destFile"
        println "(${formatWithUnits(srcFile.size())})"

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

            // keep the original file date
            destFile.lastModified = srcFile.lastModified()
        }

        (destFile.exists() ? destFile : srcFile).size()
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

    static final dateFormatter = new java.text.SimpleDateFormat('yyyy-MM-dd HH:mm:ss')

    static String formatTimestamp(long timestamp) {
        dateFormatter.format(new Date(timestamp))
    }
}
