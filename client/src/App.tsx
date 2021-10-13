import React, {useState} from "react"
import {
  Box,
  ChakraProvider,
  Flex,
  Grid,
  HStack,
  Spinner,
  Stat,
  StatLabel,
  StatNumber,
  Table,
  Tbody,
  Td,
  Th,
  Thead,
  Tr,
  VStack,
  theme,
} from "@chakra-ui/react"
import axios from "axios"
import dayjs, {Dayjs} from "dayjs"

import {useQuery} from "react-query"

import "./App.scss"
import DateRangePicker, {DateRangeObject} from "./components/DateRangePicker/DateRangePicker"

type BaseTransaction = {
  description: string
  amount: number
}

type Transaction = BaseTransaction & {
  transaction_date: Dayjs
}

type TransactionResponse = BaseTransaction & {
  transaction_date: string
}

type Stats = {
  in_ext: number;
  out_ext: number;
  delta: number;
}

type StatsBarProps = {
  stats: Stats
}

const StatsBar = ({stats}: StatsBarProps) => {
  const {
    in_ext,
    out_ext,
    delta,
  } = stats

  return (
    <HStack width="100%" textAlign="center" mb={3}>
      <Stat color="green.500">
        <StatLabel>In Ext.</StatLabel>
        <StatNumber>${in_ext}</StatNumber>
      </Stat>
      <Stat color="red.500">
        <StatLabel>Out Ext.</StatLabel>
        <StatNumber>${out_ext}</StatNumber>
      </Stat>
      <Stat>
        <StatLabel>Delta</StatLabel>
        <StatNumber>${delta}</StatNumber>
      </Stat>
    </HStack>
  )
}

type TransactionsTableProps = {
  transactions: Transaction[]
}

const formatCurrency = (val: number): string => {
  if (val < 0) {
    return `-$${Math.abs(val).toFixed(2)}`
  }

  return `$${val.toFixed(2)}`
}

const TransactionsTable = ({transactions}: TransactionsTableProps) => {
  return (
    <Table variant="simple">
      <Thead>
        <Tr>
          <Th>Description</Th>
          <Th>Amount</Th>
          <Th>Date</Th>
        </Tr>
      </Thead>
      <Tbody>
        {transactions.map(txn => (
          <Tr>
            <Td>{txn.description}</Td>
            <Td>{formatCurrency(txn.amount)}</Td>
            <Td>{txn.transaction_date.format("DD/MM/YYYY")}</Td>
          </Tr>
        ))}
      </Tbody>
    </Table>
  )
}

async function getTransactionSummary({from, to}: { from: Date, to: Date }) {
  const response = await axios.get("http://localhost:4000/api/transaction-summary", {
    params: {
      from,
      to,
    },
  })

  return {
    ...response.data,
    transactions: response.data.transactions.map((t: TransactionResponse) => ({
      ...t,
      transaction_date: dayjs(t.transaction_date),
    })),
  }
}

export const App = () => {
  const [selectedDateRange, setSelectedDateRange] = useState<DateRangeObject>({
    startDate: dayjs().startOf("day").subtract(1, "month"),
    endDate: dayjs().startOf("day"),
  })

  const transactionSummary = useQuery<{ transactions: Transaction[], stats: Stats }, Error>(
    ["transaction-summary", selectedDateRange],
    () => getTransactionSummary({
      from: selectedDateRange.startDate.toDate(),
      to: selectedDateRange.endDate.toDate(),
    }))

  const handleOnSubmit = (dateRangeObject: DateRangeObject) => {
    setSelectedDateRange(dateRangeObject)
  }

  const renderTransactionSummaryData = () => {
    if (transactionSummary.isLoading || transactionSummary.isIdle) {
      return (<Flex justifyContent="center" flexGrow={1}>
          <Spinner/>
        </Flex>
      )
    }

    if (transactionSummary.isError) {
      return null
    }

    return (
      <VStack>
        <StatsBar stats={transactionSummary.data.stats}/>
        <Box maxHeight="800" overflowY="scroll" w="100%">
          <TransactionsTable transactions={transactionSummary.data.transactions}/>
        </Box>
      </VStack>
    )
  }

  return (
    <ChakraProvider theme={theme}>
      <Grid templateColumns="1fr 3fr" height="100vh" p={5} gap={5}>
        <Box p={5} backgroundColor="gray.50" borderRadius="md">
          <DateRangePicker
            value={selectedDateRange}
            onSubmit={handleOnSubmit}/>
        </Box>
        <Box>
          <Box>
            {renderTransactionSummaryData()}
          </Box>
        </Box>
      </Grid>
    </ChakraProvider>
  )
}
