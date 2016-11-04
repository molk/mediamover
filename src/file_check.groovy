import groovy.io.FileType

files_in = { path ->
    filesByName = [:]
    (path as File).eachFileRecurse(FileType.FILES) { filesByName[it.name] = it }
    filesByName
}

Map<String,File> cpy = files_in('/Volumes/toshi_1tb/Fotos/')
Map<String,File> org = files_in('/Volumes/Leopard/Users/marcus/Desktop/bilder_done')

Map<String,File> missing = org.findAll { String orgName, File orgFile ->
    !cpy.containsKey(orgName)
}

missing.values().each { println it }

println org.size()
println missing.size()
