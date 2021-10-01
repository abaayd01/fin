import React from "react"
import { ComponentMeta } from "@storybook/react"

import DatePickerComponent from "../components/DatePicker/DatePicker"

export default {
  title: "DatePicker",
  component: DatePickerComponent,
} as ComponentMeta<typeof DatePickerComponent>

export const DatePicker = () => <DatePickerComponent />
