package br.edu.ufms.schoollab_manager

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class SchoolLabManagerApplication

fun main(args: Array<String>) {
	runApplication<SchoolLabManagerApplication>(*args)
}
