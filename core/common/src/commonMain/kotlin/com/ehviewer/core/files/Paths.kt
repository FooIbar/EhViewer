package com.ehviewer.core.files

import kotlinx.io.Sink
import kotlinx.io.Source
import okio.Path

val Path.isDirectory get() = SystemFileSystem.metadataOrNull(this)?.isDirectory == true

val Path.isFile get() = SystemFileSystem.metadataOrNull(this)?.isRegularFile == true

fun Path.metadataOrNull() = SystemFileSystem.metadataOrNull(this)

fun Path.find(name: String) = resolve(name).takeIf { it.exists() }

fun Path.exists() = SystemFileSystem.exists(this)

fun Path.delete() = SystemFileSystem.deleteRecursively(this)

fun Path.deleteContent() = SystemFileSystem.listOrNull(this)?.forEach(Path::delete)

fun Path.list() = SystemFileSystem.listOrNull(this).orEmpty()

fun Path.mkdirs() = SystemFileSystem.createDirectories(this)

infix fun Path.moveTo(target: Path) = SystemFileSystem.atomicMove(this, target)

infix fun Path.sendTo(target: Path) = SystemFileSystem.copy(this, target)

expect inline fun <T> Path.read(f: Source.() -> T): T

expect inline fun <T> Path.write(f: Sink.() -> T): T
