package fotoarchiver

import static fotoarchiver.MediaFile.assertIsExistingDirectory
import static fotoarchiver.MediaFile.copy
import static fotoarchiver.MediaFile.formatWithUnits
import static fotoarchiver.MediaFile.isVideo
import static fotoarchiver.MediaFile.timeAsString

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

sourceDir      = assertIsExistingDirectory '/Volumes/fotosundso/unsortiert/android_'
destinationDir = assertIsExistingDirectory '/Volumes/fotosundso/t'

fileFilter = new MediaFile.Filter('all')

line = '-' * 100

long startTimestamp = System.currentTimeMillis()

long filesCopied = 0
long bytesCopied = 0

println line

println "Filter : $fileFilter"
println "From   : $sourceDir"
println "To     : $destinationDir"

println line

filesToCopy = sourceDir.listFiles fileFilter

filesToCopy.each { final File srcFile ->
    def lastModified = new Date(srcFile.lastModified())

    def mediaFolder = isVideo(srcFile) ? 'videos' : 'fotos'
    def year        = lastModified[Calendar.YEAR] as String
    def month       = ((lastModified[Calendar.MONTH] + 1) as String).padLeft(2, '0')
    def destFolder  = "${destinationDir.absolutePath}/$mediaFolder/$year/$month"

    def destFile = new File(destFolder, srcFile.name)

    if (copy(srcFile: srcFile, destFile: destFile, keepSource: keepSourceFiles, dryRun: dryRun)) {
        filesCopied += 1
        bytesCopied += srcFile.size()
    }

    println line
}

def durationMillis = System.currentTimeMillis() - startTimestamp

if (!filesCopied) {
    println 'No files pushed.'
}
else {
    println "$filesCopied files in ${formatWithUnits(bytesCopied)}"
}

println "Took ${timeAsString(durationMillis)}"

