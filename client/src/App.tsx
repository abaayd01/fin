import React, { useState } from "react"
import {
  ChakraProvider,
  Container,
  HStack,
  Spinner,
  Stat,
  StatLabel,
  StatNumber,
  VStack,
  StackDivider,
  theme,
} from "@chakra-ui/react"
import axios from "axios"
import dayjs from "dayjs"
import { useQuery } from "react-query"

import "./App.scss"
import DateRangePicker, { DateRangeObject } from "./components/DateRangePicker/DateRangePicker"

async function getTransactionSummary({ from, to }: { from: Date, to: Date }) {
  const response = await axios.get("http://localhost:4000/api/transaction-summary", {
    params: {
      from,
      to,
    },
  })

  return response.data
}

export const App = () => {
  const [selectedDateRange, setSelectedDateRange] = useState<DateRangeObject>({
    startDate: dayjs().startOf("day").subtract(1, "month"),
    endDate: dayjs().startOf("day"),
  })

  const { data, isLoading } = useQuery(["transaction-summary", selectedDateRange], () => getTransactionSummary({
    from: selectedDateRange.startDate.toDate(),
    to: selectedDateRange.endDate.toDate(),
  }))

  const handleOnSubmit = (dateRangeObject: DateRangeObject) => {
    setSelectedDateRange(dateRangeObject)
  }

  return (
    <ChakraProvider theme={theme}>
      <Container my={8}>
        {isLoading ?
          <Spinner /> :
          <VStack
            divider={<StackDivider borderColor="gray.200" />}
            spacing={8}
          >
            <DateRangePicker value={selectedDateRange} onSubmit={handleOnSubmit} />
            <HStack spacing={8} textAlign="center">
              <Stat>
                <StatLabel>In</StatLabel>
                <StatNumber>${data.in}</StatNumber>
              </Stat>
              <Stat>
                <StatLabel>Out</StatLabel>
                <StatNumber>${data.out}</StatNumber>
              </Stat>
            </HStack>
          </VStack>
        }
      </Container>
    </ChakraProvider>
  )
}

