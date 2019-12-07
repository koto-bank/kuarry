package org.kotobank.kuarry.helper

internal object InventoryHelper {
    /** Runs a function, passing it a position in the inventory matrix, traversing it from left to right, top to bottom.
     *
     * @param width Width of the inventory
     * @param height Height of the inventory
     * @param func The function which is run on each position, where "position" is the slot number in the inventory, and
     *             "widthPos" & "heightPos" are the corresponding column and line in the inventory.
     *             If this function returns "true", then iteration is stopped.
     */
    fun forEachPositionInInventory(width: Int, height: Int, func: (position: Int, widthPos: Int, heightPos: Int) -> Boolean) {
        for (heightPos in 0 until height) {
            for (widthPos in 0 until width) {
                val position = (widthPos * height) + heightPos

                if (func(position, widthPos, heightPos)) return
            }
        }
    }
}