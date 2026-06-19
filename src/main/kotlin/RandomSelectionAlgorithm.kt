package com.nightlynexus

internal interface RandomNumberGenerator {
  fun integer(until: Int): Int
}

// O(exclusionIndices.size + selectCount)
internal inline fun <T> uniqueRolls(
  availableOptions: List<T>,
  exclusionIndices: Set<Int>,
  selectCount: Int,
  random: RandomNumberGenerator,
  action: (selectedElement: T) -> Unit
) {
  require(selectCount >= 0)
  val availableOptionsSize = availableOptions.size
  val exclusionIndicesSize = exclusionIndices.size
  require(availableOptionsSize - exclusionIndicesSize >= selectCount)
  // O(exclusionIndicesSize)
  val indices = getSparseArrayOfShuffledIndices(
    availableOptionsSize,
    exclusionIndices,
    selectCount
  )
  var remainingSize = availableOptionsSize - exclusionIndicesSize
  for (i in 0 until selectCount) { // O(selectCount)
    // [0, availableOptionsSize - exclusionIndicesSize - i)
    val roll = random.integer(remainingSize)
    val randomIndexOfIndices = i + roll

    val iIndex = indices[i] ?: i
    val randomIndex = indices[randomIndexOfIndices] ?: randomIndexOfIndices

    action(availableOptions[randomIndex])

    // We do not need this swap.
    // indices[i] = randomIndex
    indices[randomIndexOfIndices] = iIndex

    remainingSize--
  }
}

// O(exclusionIndices.size)
private fun getSparseArrayOfShuffledIndices(
  availableOptionsSize: Int,
  exclusionIndices: Set<Int>,
  selectCount: Int
): LinkedHashMap<Int, Int> {
  val exclusionIndicesSize = exclusionIndices.size
  val exclusionIndicesStartIndex = availableOptionsSize - exclusionIndicesSize
  var exclusionIndicesIndex = exclusionIndicesStartIndex
  val indices = LinkedHashMap<Int, Int>(exclusionIndicesSize + selectCount)
  for (exclusionIndex in exclusionIndices) { // O(exclusionIndices.size)
    require(exclusionIndex >= 0)
    require(exclusionIndex < availableOptionsSize)
    if (exclusionIndex < exclusionIndicesStartIndex) {
      // Although the inner while loop is nested inside the outer for loop, the loops do not
      // multiply.
      // We never reset exclusionIndicesIndex between iterations; exclusionIndicesIndex only ever
      // increments, starting at
      // exclusionIndicesStartIndex (availableOptionsSize - exclusionIndicesSize)
      // and advancing through
      // the tail region ([exclusionIndicesStartIndex, availableOptionsSize)).
      //
      // The tail region contains exactly exclusionIndicesSize indices, so
      // exclusionIndicesIndex can advance at most exclusionIndicesSize steps in
      // total across all iterations of the outer loop, regardless of how those
      // steps are distributed between iterations.
      //
      // Total work = outer loop (≤ exclusionIndicesSize iterations)
      //              + all inner while steps (≤ exclusionIndicesSize total)
      //            = O(x) + O(x)
      //            = O(x)
      while (exclusionIndices.contains(exclusionIndicesIndex)) {
        exclusionIndicesIndex++
      }
      indices[exclusionIndex] = exclusionIndicesIndex
      exclusionIndicesIndex++
    }
  }
  return indices
}
