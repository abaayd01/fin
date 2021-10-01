import React from "react"
import { ComponentMeta } from "@storybook/react"

import DateRangePicker from "./DateRangePicker"

export default {
  title: "DateRangePicker",
  component: DateRangePicker,
} as ComponentMeta<typeof DateRangePicker>

export const Default = () => <DateRangePicker />
