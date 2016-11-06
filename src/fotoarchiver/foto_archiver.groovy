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

sourceDir      = assertIsExistingDirectory '/Volumes/fotosundso/unsortiert/alexandra/iphone-2016-06-09'
destinationDir = assertIsExistingDirectory '/Volumes/fotosundso/t'

fileFilter = new MediaFile.Filter('all')

line = '-' * 100

println line

println "Filter : $fileFilter"
println "From   : $sourceDir"
println "To     : $destinationDir"

println line

long startTimestamp = System.currentTimeMillis()

long sumOfFilesCopied = 0
long sumOfBytesCopied = 0

sourceDir.listFiles(fileFilter).each { srcFile ->

    def bytesCopied = copy srcFile: srcFile, destFile: destFileFor(srcFile), keepSource: keepSourceFiles, dryRun: dryRun

    if (bytesCopied) {
        sumOfFilesCopied += 1
        sumOfBytesCopied += bytesCopied
    }

    println line
}

def durationMillis = System.currentTimeMillis() - startTimestamp

if (!sumOfFilesCopied) {
    println 'No files pushed.'
}
else {
    println "$sumOfFilesCopied files in ${formatWithUnits(sumOfBytesCopied)}"
}

println "Took ${timeAsString(durationMillis)}"

// helpers --------------------------------------------------------------------------

File destFileFor(File srcFile) {
    def lastModified = new Date(srcFile.lastModified())

    def mediaFolder = isVideo(srcFile) ? 'videos' : 'fotos'
    def year        = lastModified[Calendar.YEAR].toString()
    def month       = (lastModified[Calendar.MONTH] + 1).toString().padLeft(2, '0')
    def destFolder  = "${destinationDir.absolutePath}/$mediaFolder/$year/$month"

    new File(destFolder, srcFile.name)
}
