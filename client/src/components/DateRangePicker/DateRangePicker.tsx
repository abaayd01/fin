import React, { useEffect, useState } from "react"
import { Button, FormControl, FormLabel, HStack, NumberInput, NumberInputField, VStack } from "@chakra-ui/react"
import dayjs, { Dayjs } from "dayjs"

const objectSupport = require("dayjs/plugin/objectSupport")
dayjs.extend(objectSupport)

type DatePickerProps = {
  onChange?: (val: Dayjs) => void
  initialValue: Dayjs
}

type DateObject = {
  year: number,
  month: number,
  day: number
}

const DatePicker = ({ onChange, initialValue }: DatePickerProps) => {
  const [{ year, month, day }, setDate] = useState<DateObject>({
    year: initialValue.year(),
    month: initialValue.month(),
    day: initialValue.date(),
  })

  useEffect(() => {
    if (onChange) {
      // @ts-ignore
      const date = dayjs({ year, month, day })
      onChange(date)
    }
  }, [year, month, day, onChange])

  return (
    <HStack width="md">
      <FormControl>
        <FormLabel>Year</FormLabel>
        <NumberInput
          min={0}
          value={year}
          onChange={val => setDate(prev => ({
            ...prev,
            year: parseInt(val),
          }))}>
          <NumberInputField placeholder="year" />
        </NumberInput>
      </FormControl>
      <FormControl>
        <FormLabel>Month</FormLabel>
        <NumberInput
          min={1}
          max={12}
          value={month}
          onChange={val => setDate(prev => ({
            ...prev,
            month: parseInt(val),
          }))}>
          <NumberInputField placeholder="month" />
        </NumberInput>
      </FormControl>
      <FormControl>
        <FormLabel>Day</FormLabel>
        <NumberInput
          min={1}
          max={31}
          value={day}
          onChange={val => setDate(prev => ({
            ...prev,
            day: parseInt(val),
          }))}>
          <NumberInputField placeholder="day" />
        </NumberInput>
      </FormControl>
    </HStack>
  )
}

type DateRangeObject = {
  startDate: Dayjs
  endDate: Dayjs
}

const DateRangePickerDefaultProps = {
  onSubmit: (_: DateRangeObject) => null,
}

type DateRangePickerProps = {
  onSubmit?: (dateRange: DateRangeObject) => void
} & typeof DateRangePickerDefaultProps

const now = dayjs()

const DateRangePicker = ({ onSubmit }: DateRangePickerProps) => {
  const [startDate, setStartDate] = useState<Dayjs>(now)
  const [endDate, setEndDate] = useState<Dayjs>(now)

  return (
    <VStack align="left">
      <FormControl id="start-date">
        <FormLabel>Start Date</FormLabel>
        <DatePicker onChange={setStartDate} initialValue={startDate} />
      </FormControl>
      <FormControl id="end-date">
        <FormLabel>End Date</FormLabel>
        <DatePicker onChange={setEndDate} initialValue={endDate} />
      </FormControl>
      <Button width="md" onClick={() => onSubmit({ startDate, endDate })}>Go</Button>
    </VStack>
  )
}

DateRangePicker.defaultProps = DateRangePickerDefaultProps

export default DateRangePicker
