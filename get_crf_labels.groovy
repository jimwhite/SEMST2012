#!/usr/bin/env groovy

new File(args[0]).eachLine {
    if (it) {
        print (it.split(' ')[-1])
        println ' '
    } else {
        println()
    }
}
