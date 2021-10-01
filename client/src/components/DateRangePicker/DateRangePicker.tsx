import React from "react"
import { Button, HStack, NumberInput, NumberInputField } from "@chakra-ui/react"

const DateRangePicker = () => {
  return (
    <HStack width="md">
      <NumberInput min={0}>
        <NumberInputField placeholder="year" />
      </NumberInput>
      <NumberInput min={1} max={12}>
        <NumberInputField placeholder="month" />
      </NumberInput>
      <NumberInput min={1} max={31}>
        <NumberInputField placeholder="day" />
      </NumberInput>
    </HStack>
  )
}

export default DateRangePicker
