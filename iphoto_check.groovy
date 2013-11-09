import groovy.io.FileType

static final iPhotoRoot() { '/Volumes/Leopard/Users/marcus/Pictures/iPhoto Library/Originals' }

println "Base directory: ${iPhotoRoot()}"

collectImagesIn (iPhotoRoot()) .with {
    filesByName.each { String filename, List<File> files ->
        if (files.size() == 1) {
            println asString(files.first())
        }
        else {
            println "Duplicate files found for $filename"

            files.each { file -> println "\t${asString(file)}" }
        }
    }

    println "$fileCount files found."
    println "$dupesCount dupicate files found."
}

static ImagesFound collectImagesIn(final path) {
    final imagesFound = new ImagesFound()

    imagesFound.with {
        (path as File).eachFileRecurse(FileType.FILES) { final File file ->
            if (!filesByName.containsKey(file.name)) {
                filesByName[file.name] = [file]
            }
            else {
                filesByName[file.name] << file
                dupesCount += 1
            }
            fileCount += 1
        }
    }

    return imagesFound
}

static String asString (File file) {
    "${file.name.padLeft(15)} ${format(file.lastModified())} ${toNumInUnits(file.size()).padLeft(10)}  ${file.absolutePath - (iPhotoRoot() + '/') - file.name}"
}

static String format(long timestamp) {
    new Date(timestamp).format('yyyy-MM-dd hh:mm:ss')
}

static String toNumInUnits(long bytes) {
    int u = 0
    for (;bytes > 1024*1024; bytes >>= 10) u++
    if (bytes > 1024) u++
    String.format('%.2f %cB', bytes/1024f, ' KMGTPE'.charAt(u))
}

class ImagesFound {
    int fileCount = 0
    int dupesCount = 0

    final filesByName = [:]
}
