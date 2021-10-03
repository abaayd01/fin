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
        <VStack
          divider={<StackDivider borderColor="gray.200" />}
          spacing={8}
        >
          <DateRangePicker value={selectedDateRange} onSubmit={handleOnSubmit} />
          {isLoading ?
            <Spinner /> :
            (
              <VStack textAlign="center">
                <HStack spacing={8} textAlign="center">
                  <Stat color="green.500" size="xs">
                    <StatLabel>In</StatLabel>
                    <StatNumber>${data.in}</StatNumber>
                  </Stat>
                  <Stat color="red.500" size="xs">
                    <StatLabel>Out</StatLabel>
                    <StatNumber>${data.out}</StatNumber>
                  </Stat>
                </HStack>
                <HStack spacing={8} textAlign="center">
                  <Stat color="green.500" size="xs">
                    <StatLabel>In Ext.</StatLabel>
                    <StatNumber>${data.in_ext}</StatNumber>
                  </Stat>
                  <Stat color="red.500" size="xs">
                    <StatLabel>Out Ext.</StatLabel>
                    <StatNumber>${data.out_ext}</StatNumber>
                  </Stat>
                </HStack>
                <Stat>
                  <StatLabel>Delta</StatLabel>
                  <StatNumber>${data.delta}</StatNumber>
                </Stat>
              </VStack>
            )
          }
        </VStack>
      </Container>
    </ChakraProvider>
  )
}

