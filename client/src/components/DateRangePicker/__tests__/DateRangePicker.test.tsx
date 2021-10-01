import React from "react"
import { render, screen } from "@testing-library/react"
import DateRangePicker from "../DateRangePicker"
import userEvent from "@testing-library/user-event"
import dayjs from "dayjs"

const objectSupport = require("dayjs/plugin/objectSupport")
dayjs.extend(objectSupport)

describe("DateRangePicker", () => {
  it("renders", () => {
    render(<DateRangePicker value={{ startDate: dayjs(), endDate: dayjs() }} />)
  })

  it("calls the onSubmit callback with the given start date and end date, when the 'go' button is clicked", async () => {
    const spy = jest.fn()
    const expectedStartDayjsDate = dayjs("2020-01-01")
    const expectedEndDayjsDate = dayjs("2021-12-31")

    render(<DateRangePicker
      value={{ startDate: expectedStartDayjsDate, endDate: expectedEndDayjsDate }} onSubmit={spy} />)

    const btn = await screen.findByRole("button", { name: /go/i })

    userEvent.click(btn)

    expect(spy).toBeCalledWith({ startDate: expectedStartDayjsDate, endDate: expectedEndDayjsDate })
  })

  it("calls the onSubmit callback with the input start date and end date, when the 'go' button is clicked", async () => {
    const spy = jest.fn()
    const expectedStartDayjsDate = dayjs("2020-01-01")
    const expectedEndDayjsDate = dayjs("2021-12-31")

    render(<DateRangePicker value={{ startDate: dayjs(), endDate: dayjs() }} onSubmit={spy} />)

    const [startYearInput, endYearInput] = await screen.findAllByLabelText(/year/i)
    const [startMonthInput, endMonthInput] = await screen.findAllByLabelText(/month/i)
    const [startDayInput, endDayInput] = await screen.findAllByLabelText(/day/i)

    userEvent.clear(startYearInput)
    userEvent.type(startYearInput, "2020")
    userEvent.clear(startMonthInput)
    userEvent.type(startMonthInput, "1")
    userEvent.clear(startDayInput)
    userEvent.type(startDayInput, "1")

    userEvent.clear(endYearInput)
    userEvent.type(endYearInput, "2021")
    userEvent.clear(endMonthInput)
    userEvent.type(endMonthInput, "12")
    userEvent.clear(endDayInput)
    userEvent.type(endDayInput, "31")

    const btn = await screen.findByRole("button", { name: /go/i })

    userEvent.click(btn)

    expect(spy).toBeCalledWith({ startDate: expectedStartDayjsDate, endDate: expectedEndDayjsDate })
  })
})
