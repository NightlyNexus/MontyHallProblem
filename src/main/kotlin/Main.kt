package com.nightlynexus

import kotlin.random.Random
import kotlinx.browser.document
import kotlinx.dom.createElement
import org.w3c.dom.Document
import org.w3c.dom.Element
import org.w3c.dom.HTMLButtonElement
import org.w3c.dom.HTMLElement
import org.w3c.dom.HTMLFormElement
import org.w3c.dom.HTMLInputElement
import org.w3c.dom.events.Event
import org.w3c.dom.events.EventListener

private const val firstSelectedColor = "#2C387E"
private const val firstUnselectedColor = "#FFC107"
private const val goatColor = "#AA2E25"
private const val firstUnselectedUnrevealedColor = "#FF5722"
private const val secondSelectedColor = "#00BCD4"

fun main() {
  val grid = document.getElementById("grid-container")!!
  val firstSelectedChanceParent =
    document.getElementById("first-selected-chance-parent") as HTMLElement
  val firstSelectedChance = document.getElementById("first-selected-chance") as HTMLElement
  val firstUnselectedChance = document.getElementById("first-unselected-chance") as HTMLElement
  val goatChanceParent = document.getElementById("goat-chance-parent") as HTMLElement
  val goatChance = document.getElementById("goat-chance") as HTMLElement
  val firstUnselectedNoGoatChance =
    document.getElementById("first-unselected-no-goat-chance") as HTMLElement
  val secondSelectedChanceParent =
    document.getElementById("second-selected-chance-parent") as HTMLElement
  val secondSelectedChance = document.getElementById("second-selected-chance") as HTMLElement
  val continueButton = document.getElementById("continue-button") as HTMLButtonElement

  val inputForm = document.getElementById("input-form") as HTMLFormElement
  val doorCountInput = document.getElementById("door-count") as HTMLInputElement
  val goatRevealCountInput = document.getElementById("goat-reveal-count") as HTMLInputElement
  val errorMessage = document.getElementById("input-form-error-message")!!

  val random = object : Game.RandomNumberGenerator {
    override fun integer(until: Int): Int {
      return Random.nextInt(until)
    }
  }

  val game = Game(
    grid,
    firstSelectedChanceParent,
    firstSelectedChance,
    firstUnselectedChance,
    goatChanceParent,
    goatChance,
    firstUnselectedNoGoatChance,
    secondSelectedChanceParent,
    secondSelectedChance,
    continueButton,
    random
  )

  inputForm.onsubmit = { event ->
    event.preventDefault()
    val doorCount = doorCountInput.value.toIntOrNull() ?: 0
    val goatRevealCount = goatRevealCountInput.value.toIntOrNull() ?: 0

    if (goatRevealCount < 1) {
      errorMessage.textContent = "Goat reveals must be >= 1"
    } else if (doorCount < goatRevealCount + 2) {
      errorMessage.textContent = "Doors must be >= Goat reveals + 2"
    } else {
      errorMessage.textContent = ""
      game.start(doorCount, goatRevealCount)
    }
  }

  val doorCount = doorCountInput.value.toInt()
  val goatRevealCount = goatRevealCountInput.value.toInt()

  game.start(doorCount, goatRevealCount)
}

private class Game(
  private val grid: Element,
  private val firstSelectedChanceParent: HTMLElement,
  private val firstSelectedChance: HTMLElement,
  private val firstUnselectedChance: HTMLElement,
  private val goatChanceParent: HTMLElement,
  private val goatChance: HTMLElement,
  private val firstUnselectedNoGoatChance: HTMLElement,
  private val secondSelectedChanceParent: HTMLElement,
  private val secondSelectedChance: HTMLElement,
  private val continueButton: HTMLButtonElement,
  private val random: RandomNumberGenerator
) {
  interface RandomNumberGenerator {
    fun integer(until: Int): Int
  }

  private var continueButtonClickListener: EventListener? = null

  fun start(doorCount: Int, goatRevealCount: Int) {
    while (true) {
      val gridChild = grid.firstChild ?: break
      grid.removeChild(gridChild)
    }

    val allDoors = ArrayList<Door>(doorCount)
    val ownerDocument = grid.ownerDocument!!
    for (i in 1..doorCount) {
      val door = Door(ownerDocument)
      grid.appendChild(door.element)
      allDoors += door
    }

    start(allDoors, goatRevealCount)
  }

  private fun start(allDoors: List<Door>, goatRevealCount: Int) {
    firstSelectedChanceParent.style.display = "none"
    goatChanceParent.style.display = "none"
    secondSelectedChanceParent.style.display = "none"

    continueButton.disabled = true

    var firstSelectedDoorIndex = -1
    setContinueButtonClickListener(object : EventListener {
      override fun handleEvent(event: Event) {
        check(firstSelectedDoorIndex != -1)
        openGoat(allDoors, firstSelectedDoorIndex, goatRevealCount)
      }
    })
    val doorCount = allDoors.size
    for (i in allDoors.indices) {
      val door = allDoors[i]
      door.setClickListener(object : EventListener {
        override fun handleEvent(event: Event) {
          firstSelectedDoorIndex = i
          for (otherDoor in allDoors) {
            otherDoor.firstSelected = otherDoor === door
          }

          firstSelectedChanceParent.style.display = "initial"
          firstSelectedChance.textContent = run {
            val chance = 100.0 / doorCount
            val formattedChance = chance.formatPercentage()
            val equalsChanceSign = if (chance == formattedChance) '=' else '≈'
            "1 / $doorCount  $equalsChanceSign  $formattedChance%"
          }
          firstUnselectedChance.textContent = run {
            val chance = 100.0 * (doorCount - 1) / doorCount
            val formattedChance = chance.formatPercentage()
            val equalsChanceSign = if (chance == formattedChance) '=' else '≈'
            "($doorCount - 1) / $doorCount  =  ${doorCount - 1} / $doorCount  " +
              "$equalsChanceSign  $formattedChance%"
          }

          continueButton.disabled = false
        }
      })
    }
  }

  private fun openGoat(allDoors: List<Door>, firstSelectedDoorIndex: Int, goatRevealCount: Int) {
    val doorCount = allDoors.size
    val prizeDoorIndex = random.integer(doorCount)

    openGoatDoors(allDoors, firstSelectedDoorIndex, prizeDoorIndex, goatRevealCount)
    for (door in allDoors) {
      if (!door.firstSelected && !door.isOpenedGoat) {
        door.setFirstUnselectedUnrevealed()
      }
    }

    continueButton.disabled = true

    goatChanceParent.style.display = "initial"
    goatChance.textContent = run {
      "(0) (($doorCount - 1) / $doorCount)  =  (0) (${doorCount - 1} / $doorCount)  =  0%"
    }
    firstUnselectedNoGoatChance.textContent = run {
      val chance = 100.0 * (doorCount - 1) / doorCount
      val formattedChance = chance.formatPercentage()
      val equalsChanceSign = if (chance == formattedChance) '=' else '≈'
      "(($doorCount - 1 - $goatRevealCount) / ($doorCount - 1 - $goatRevealCount))" +
        " (($doorCount - 1) / $doorCount)  =  " +
        "(${doorCount - 1 - goatRevealCount} / ${doorCount - 1 - goatRevealCount})" +
        " (${doorCount - 1} / $doorCount)  $equalsChanceSign  $formattedChance%"
    }

    var secondSelectedDoorIndex = -1
    setContinueButtonClickListener(object : EventListener {
      override fun handleEvent(event: Event) {
        val selectedDoor = allDoors[secondSelectedDoorIndex]
        check(!selectedDoor.isOpenedGoat && selectedDoor.secondSelected)
        complete(allDoors, prizeDoorIndex)
      }
    })

    for (i in allDoors.indices) {
      val door = allDoors[i]
      door.setClickListener(object : EventListener {
        override fun handleEvent(event: Event) {
          if (allDoors[i].isOpenedGoat) {
            return
          }
          secondSelectedDoorIndex = i
          for (j in allDoors.indices) {
            val otherDoor = allDoors[j]
            if (!allDoors[j].isOpenedGoat) {
              otherDoor.secondSelected = i == j
            }
          }

          secondSelectedChanceParent.style.display = "initial"
          if (secondSelectedDoorIndex == firstSelectedDoorIndex) {
            val chance = 100.0 / doorCount
            val formattedChance = chance.formatPercentage()
            val equalsChanceSign = if (chance == formattedChance) '=' else '≈'
            secondSelectedChance.textContent = run {
              "(1 / 1) (1 / $doorCount)  =  1 / $doorCount  $equalsChanceSign  " +
                "${formattedChance}%"
            }
          } else {
            secondSelectedChance.textContent = run {
              val chance =
                100.0 * (doorCount - 1) / (doorCount - 1 - goatRevealCount) / doorCount
              val formattedChance = chance.formatPercentage()
              val equalsChanceSign = if (chance == formattedChance) '=' else '≈'
              "(1 / ($doorCount - 1 - $goatRevealCount)) (($doorCount - 1) / $doorCount)  =  " +
                "(1 / ${doorCount - 1 - goatRevealCount}) (${doorCount - 1} / $doorCount)  " +
                "$equalsChanceSign  ${formattedChance}%"
            }
          }

          continueButton.disabled = false
        }
      })
    }
  }

  private fun complete(allDoors: List<Door>, prizeDoorIndex: Int) {
    continueButton.disabled = true
    removeContinueButtonClickListener()

    for (i in allDoors.indices) {
      val door = allDoors[i]
      door.removeClickListener()
      door.complete(i == prizeDoorIndex)
    }
  }

  private fun openGoatDoors(
    allDoors: List<Door>,
    firstSelectedDoorIndex: Int,
    prizeDoorIndex: Int,
    goatRevealCount: Int
  ) {
    require(goatRevealCount >= 1)
    val doorCount = allDoors.size
    require(doorCount >= goatRevealCount + 2)
    val sortedSelection = if (firstSelectedDoorIndex == prizeDoorIndex) {
      ArrayList<Int>(goatRevealCount + 1).apply {
        add(firstSelectedDoorIndex)
      }
    } else {
      ArrayList<Int>(goatRevealCount + 2).apply {
        if (firstSelectedDoorIndex > prizeDoorIndex) {
          add(prizeDoorIndex)
          add(firstSelectedDoorIndex)
        } else {
          add(firstSelectedDoorIndex)
          add(prizeDoorIndex)
        }
      }
    }
    for (i in 0 until goatRevealCount) {
      var roll = random.integer(doorCount - sortedSelection.size)
      var j = 0
      while (j < sortedSelection.size) {
        val oldRoll = sortedSelection[j]
        if (roll < oldRoll) {
          break
        }
        roll++
        j++
      }
      sortedSelection.add(j, roll)
      allDoors[roll].openGoat()
    }
  }

  private fun setContinueButtonClickListener(listener: EventListener) {
    val old = continueButtonClickListener
    if (old != null) {
      continueButton.removeEventListener("click", old)
    }
    continueButtonClickListener = listener
    continueButton.addEventListener("click", listener)
  }

  private fun removeContinueButtonClickListener() {
    continueButton.removeEventListener("click", continueButtonClickListener!!)
    continueButtonClickListener = null
  }

  private fun Double.formatPercentage(): Double {
    val percentage = this
    return js("+parseFloat(percentage).toFixed(4)") as Double
  }

  private class Door(ownerDocument: Document) {
    var firstSelected: Boolean = false
      set(value) {
        field = value
        if (value) {
          element.setAttribute("style", "background-color: $firstSelectedColor;")
        } else {
          element.setAttribute("style", "background-color: ${firstUnselectedColor};")
        }
      }
    var secondSelected: Boolean = false
      set(value) {
        field = value
        if (value) {
          spanElement.setAttribute("style", "background-color: $secondSelectedColor;")
        } else {
          spanElement.setAttribute("style", "background-color: transparent;")
        }
      }
    var isOpenedGoat = false
      private set

    private val spanElement = ownerDocument.createElement("span") {
      textContent = "🚪"
      className = "emoji-image"
    }
    private val secondBox = ownerDocument.createElement("div") {
      className = "second-box"
    }
    val element = ownerDocument.createElement("div") {
      className = "option-box"
      appendChild(secondBox)
      appendChild(spanElement)
    }

    fun openGoat() {
      check(!isOpenedGoat)
      spanElement.textContent = "🐐"
      secondBox.setAttribute("style", "background-color: $goatColor;")
      isOpenedGoat = true
    }

    fun setFirstUnselectedUnrevealed() {
      check(!isOpenedGoat)
      secondBox.setAttribute("style", "background-color: $firstUnselectedUnrevealedColor;")
    }

    fun complete(prize: Boolean) {
      spanElement.textContent = if (prize) "🏆" else "🐐"
    }

    private var clickListener: EventListener? = null

    fun setClickListener(listener: EventListener) {
      val old = clickListener
      if (old != null) {
        element.removeEventListener("click", old)
      }
      clickListener = listener
      element.addEventListener("click", listener)
    }

    fun removeClickListener() {
      element.removeEventListener("click", clickListener!!)
      clickListener = null
    }
  }
}
