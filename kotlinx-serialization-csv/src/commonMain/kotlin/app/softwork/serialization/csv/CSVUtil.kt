package app.softwork.serialization.csv

import kotlinx.serialization.*
import kotlinx.serialization.descriptors.*

@OptIn(ExperimentalSerializationApi::class)
internal val SerialDescriptor.names: Sequence<String>
    get() = sequence {
        elementDescriptors.forEachIndexed { index, descriptor ->
            val name = getElementName(index)
            when {
                descriptor.elementsCount == 0 -> yield(name)
                descriptor.kind == SerialKind.ENUM -> yield(name)
                descriptor.kind is StructureKind.MAP -> yield(name)
                descriptor.kind is StructureKind.LIST -> yield(name)
                else -> yieldAll(descriptor.names)
            }
        }
    }

@ExperimentalSerializationApi
internal fun SerialDescriptor.checkForLists() {
    for (descriptor in elementDescriptors) {
        if (descriptor.kind is StructureKind.LIST || descriptor.kind is StructureKind.MAP) {
            error("List or Map are not yet supported")
        }
        descriptor.checkForLists()
    }
}
