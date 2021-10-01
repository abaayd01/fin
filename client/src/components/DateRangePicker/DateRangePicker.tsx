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
  year: number | string,
  month: number | string,
  day: number | string
}

const DatePicker = ({ onChange, initialValue }: DatePickerProps) => {
  const [{ year, month, day }, setDate] = useState<DateObject>({
    year: initialValue.year(),
    month: initialValue.month() + 1,
    day: initialValue.date(),
  })

  useEffect(() => {
    if (onChange) {
      // @ts-ignore
      const date = dayjs({ year, month: month - 1, day })
      onChange(date)
    }
  }, [year, month, day, onChange])

  return (
    <HStack width="md">
      <FormControl>
        <FormLabel fontSize="xs">Year</FormLabel>
        <NumberInput
          min={0}
          value={year}
          onChange={val => setDate(prev => ({
            ...prev,
            year: parseInt(val) || val,
          }))}>
          <NumberInputField placeholder="year" />
        </NumberInput>
      </FormControl>
      <FormControl>
        <FormLabel fontSize="xs">Month</FormLabel>
        <NumberInput
          min={1}
          max={12}
          value={month}
          onChange={val => setDate(prev => ({
            ...prev,
            month: parseInt(val) || val,
          }))}>
          <NumberInputField placeholder="month" />
        </NumberInput>
      </FormControl>
      <FormControl>
        <FormLabel fontSize="xs">Day</FormLabel>
        <NumberInput
          min={1}
          max={31}
          value={day}
          onChange={val => setDate(prev => ({
            ...prev,
            day: parseInt(val) || val,
          }))}>
          <NumberInputField placeholder="day" />
        </NumberInput>
      </FormControl>
    </HStack>
  )
}

export type DateRangeObject = {
  startDate: Dayjs
  endDate: Dayjs
}

const DateRangePickerDefaultProps = {
  onSubmit: (_: DateRangeObject) => {
  },
}

type DateRangePickerProps = {
  onSubmit?: (dateRange: DateRangeObject) => void
} & typeof DateRangePickerDefaultProps

const now = dayjs()

const DateRangePicker = ({ onSubmit }: DateRangePickerProps) => {
  const [startDate, setStartDate] = useState<Dayjs>(now)
  const [endDate, setEndDate] = useState<Dayjs>(now)

  return (
    <VStack align="left" spacing={4}>
      <FormControl id="start-date">
        <FormLabel>Start Date</FormLabel>
        <DatePicker onChange={setStartDate} initialValue={now} />
      </FormControl>
      <FormControl id="end-date">
        <FormLabel>End Date</FormLabel>
        <DatePicker onChange={setEndDate} initialValue={now} />
      </FormControl>
      <Button width="md" onClick={() => onSubmit({ startDate, endDate })}>Go</Button>
    </VStack>
  )
}

DateRangePicker.defaultProps = DateRangePickerDefaultProps

export default DateRangePicker
