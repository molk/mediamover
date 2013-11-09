
println appendVersionTo('foo_v2.b')

String appendVersionTo(String name) {
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

String suffixOf(final String name) {
    final int i = name.lastIndexOf(".")
    (i < 0 || i == name.length()-1) ? '' : name.substring(i)
}
