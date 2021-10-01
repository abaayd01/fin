import React from "react"
import { ComponentMeta } from "@storybook/react"

import DatePicker from "./DatePicker"

export default {
  title: "DatePicker",
  component: DatePicker,
} as ComponentMeta<typeof DatePicker>

export const Default = () => <DatePicker />
