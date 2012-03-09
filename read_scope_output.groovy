#!/usr/bin/env groovy

sys_label = [:]

new File('data/split_test_output.txt').eachLine {
    def fields = it.split(/\s+/)
    def classes = (fields.tail() as List).collate(2).collect { [it[0], it[1] as Double]}
//    println classes
    def best = classes.max { it[1] }

    sys_label[fields[0]] = best[0]
}

new File('data/split_test.up_scope.txt').eachLine {
    def fields = it.split(/\s+/)
    println "${sys_label[fields[0]]} $it"
}