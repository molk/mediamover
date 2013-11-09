import groovy.io.FileType

final iPhotoRoot = '/Volumes/Leopard/Users/marcus/Pictures/iPhoto Library/Originals'

final line = '-' * 80

println line
println "Base directory: $iPhotoRoot"
println line

FilesByTypeYearAndMonth.createWithRootPath(iPhotoRoot).with {

    def print = { params ->
        println "\n$params.title:"

        params.collector.years.each { year ->
            MonthCollector monthsForYear = params.collector.monthCollectorFor(year)

            println "$year (${params.collector.statisticsFor(year).summary})"

            monthsForYear.months.each { month ->
                println "\t${(month as String).padLeft(2)} (${monthsForYear.statisticsFor(month).summary})"
                if (params.printFiles) {
                    monthsForYear.filesFor(month).each { file ->
                        println "\t\t${Util.asString(file)}"
                    }
                }
            }
        }
    }

    print ( title: 'Photos', collector: photos, printFiles: true )
    print ( title: 'Videos', collector: videos, printFiles: true )

    print ( title: 'Photos', collector: photos )
    print ( title: 'Videos', collector: videos )

    println '\nSummary --------------------------------------'
    println "Photos : $photos.statistics.summary"
    println "Videos : $videos.statistics.summary"
    println "Total  : $summary"
    println '----------------------------------------------'
}

class Statistics {
    long count
    long bytes

    def leftShift(File file) {
        count += 1
        bytes += file.size()
    }

    String getSummary() { Util.summaryFor(count, bytes) }
}

class YearCollector {

    final Map<Integer, MonthCollector> monthCollectorByYear  = [:]
    final Map<Integer, Statistics>     statisticsByYear      = [:]

    final Statistics statistics = new Statistics()

    List<Integer> getYears() {
        monthCollectorByYear.keySet().sort().asList()
    }

    MonthCollector monthCollectorFor(Integer year) { monthCollectorByYear[year] }
    Statistics     statisticsFor(Integer year)     { statisticsByYear[year]     }
    Statistics     getStatistics()                 { statistics                 }

    def leftShift(File file) {
        final year = new Date(file.lastModified())[Calendar.YEAR]

        if (!monthCollectorByYear[year]) {
            monthCollectorByYear[year] = new MonthCollector()
            statisticsByYear[year] = new Statistics()
        }

        monthCollectorByYear[year] << file
        statisticsByYear[year]     << file
        statistics                 << file
    }
}

class MonthCollector {
    final Map<Integer, List<File>> filesByMonth = [:]
    final Map<Integer, Statistics> statisticsByMonth = [:]

    List<Integer> getMonths() {
        filesByMonth.keySet().sort().asList()
    }

    List<File> filesFor(Integer month)      { filesByMonth[month] }
    Statistics statisticsFor(Integer month) { statisticsByMonth[month] }

    def leftShift(File file) {
        final lastModified = new Date(file.lastModified())
        final month = lastModified[Calendar.MONTH] + 1

        if (!filesByMonth[month]) {
            filesByMonth[month] = []
            statisticsByMonth[month] = new Statistics()
        }

        filesByMonth[month]      << file
        statisticsByMonth[month] << file
    }
}

class FilesByTypeYearAndMonth {
    static final videoSuffixes = ['mov', 'avi', 'mp4', 'm4v', 'mpg', 'mpeg']

    static FilesByTypeYearAndMonth createWithRootPath(path) {
        def instance = new FilesByTypeYearAndMonth()
        (path as File).eachFileRecurse(FileType.FILES) { file -> instance << file }
        return instance
    }

    YearCollector photos = new YearCollector()
    YearCollector videos = new YearCollector()

    def leftShift(File file) {
        (isVideo(file) ? videos : photos) << file
    }

    String getSummary() {
        def collectors = [photos, videos]

        def totalFileCount = collectors.collect { it.statistics.count } .sum()
        def totalFileSizes = collectors.collect { it.statistics.bytes } .sum()

        Util.summaryFor(totalFileCount, totalFileSizes)
    }

    private static boolean isVideo(File file) {
        final suffix = file.name.toLowerCase().tokenize('.').last()
        videoSuffixes.any { it == suffix }
    }
}

class Util {

    static String summaryFor(long count, long sizes) {
        "$count file${count > 1 ? 's' : ''} in ${Util.toNumInUnits(sizes)}"
    }

    static String toNumInUnits(long bytes) {
        int u = 0

        for (;bytes > 1024*1024; bytes >>= 10) u++
        if (bytes > 1024) u++

        String.format('%.2f %cB', bytes/1024f, ' KMGTPE'.charAt(u))
    }

    static String asString (File file) {
        "${file.name.padLeft(15)} ${format(file.lastModified())} ${toNumInUnits(file.size()).padLeft(10)}  ${file.absolutePath - file.name}"
    }

    static String format(long timestamp) {
        new Date(timestamp).format('yyyy-MM-dd HH:mm:ss')
    }
}
