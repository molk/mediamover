@Grab('com.drewnoakes:metadata-extractor:2.9.1')

import java.nio.file.Files
import java.nio.file.attribute.BasicFileAttributeView
import java.nio.file.attribute.BasicFileAttributes
import java.nio.file.attribute.FileTime
import java.util.concurrent.TimeUnit

import com.drew.imaging.jpeg.JpegMetadataReader
import com.drew.metadata.exif.ExifReader
import com.drew.metadata.exif.ExifSubIFDDirectory
import groovy.io.FileType

/**
 * Sets 0 file dates (1.1.1970) to a date according to the directory struecture.
 *
 * @author Marcus Olk
 *
 * 26.11.2016
 */

rootDir = '/Volumes/fotosundso/t2'
// t_1.1.1970/
// --------------------------------------------------------------------------------------------------------------------------------

def n = 0
def h = 0

readJpgCreationDateFrom = { File file ->
    try {
        JpegMetadataReader
            .readMetadata(file, [new ExifReader()])
            .getDirectory(ExifSubIFDDirectory)
            .getDate(ExifSubIFDDirectory.TAG_DATETIME_ORIGINAL)
    }
    catch (failure) {
        null
    }
}

new File(rootDir).eachFileRecurse(FileType.FILES) { file ->
    def toMillis   = { it?.to TimeUnit.MILLISECONDS }

    def millisFromFile = { File f ->
        // .../videos/2016/10/IMG_6527.MOV -> 2016-10
        def exifDate = (f.name.toLowerCase().endsWith('.jpg')) ? readJpgCreationDateFrom(f) : null

        if (exifDate) exifDate.time
        else {
            try {
                def dateString = ((file.parent - file.parentFile.parentFile.parent).split('/') - '').join('-')
                Date.parse('yyyy-MM', dateString).time
            }
            catch (failure) {
                0
            }
        }
    }

    def s = { Long ms -> new Date(ms).format('yyyy-MM-dd HH:mm:ss') }

    def dates = { c,a,m ->
        "created: ${s(c)}, last modified: ${s(m)}, last access: ${s(a)}"
    }

    def attributes = Files.readAttributes file.toPath(), BasicFileAttributes

    def created = toMillis(attributes.creationTime())
    def lastAccess = toMillis(attributes.lastAccessTime())
    def lastModified = toMillis(attributes.lastModifiedTime())

    n++

    if (created == 0 && lastModified == 0) {
        h++
        println "${file.absolutePath}: ${dates created, lastAccess, lastModified}"

        def msFromFile = millisFromFile(file)

        if (msFromFile) {
            created      = FileTime.fromMillis(msFromFile)
            lastModified = created
            lastAccess = FileTime.fromMillis(System.currentTimeMillis())

            println "Created ${s(created.toMillis())}, last access ${s(lastAccess.toMillis())}, last modified ${s(lastModified.toMillis())}"
            println '-' * 200

            Files.getFileAttributeView(file.toPath(), BasicFileAttributeView)
                .setTimes(lastModified, lastAccess, created)
        }
    }
}

println "$h of $n files fixed"
