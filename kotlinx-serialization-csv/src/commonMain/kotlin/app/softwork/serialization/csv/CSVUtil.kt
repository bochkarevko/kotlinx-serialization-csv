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

/**
 * Checks that all descriptors inside this one (if there are any) can be put into a single column.
 * @see SerialDescriptor.isTrivial
 */
@ExperimentalSerializationApi
internal fun SerialDescriptor.canBeFlattened(): Boolean =
    elementDescriptors.all { descriptor ->
        when (descriptor.kind) {
            is PrimitiveKind -> true
            is SerialKind.ENUM -> true
            is SerialKind.CONTEXTUAL -> true
            StructureKind.LIST -> descriptor.elementDescriptors.first().isTrivial()
            StructureKind.MAP -> descriptor.elementDescriptors.first().isTrivial()
            else -> {
                val inside = descriptor.elementDescriptors.toList()
                inside.all { it.canBeFlattened() }
            }
        }
    }

/**
 * Checks that this descriptor can be put into a single column. If not, then it cannot be safely put into a csv.
 */
@ExperimentalSerializationApi
internal fun SerialDescriptor.isTrivial(): Boolean =
    elementDescriptors.all { descriptor ->
        when (descriptor.kind) {
            is PrimitiveKind -> true
            StructureKind.LIST -> descriptor.elementDescriptors.first().isTrivial()
            StructureKind.MAP -> error { "Maps are not supported yet" } // TODO: Map entry is not that trivial
            else -> descriptor.elementsCount == 0 || (descriptor.elementsCount == 1 && descriptor.elementDescriptors.first()
                .isTrivial())
        }
    }
