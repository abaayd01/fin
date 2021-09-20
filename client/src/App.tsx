import React from "react"
import {
  Box,
  Center,
  ChakraProvider,
  Grid,
  HStack,
  Spinner,
  Stat,
  StatLabel,
  StatNumber,
  VStack,
  theme,
} from "@chakra-ui/react"
import axios from "axios"
import dayjs from "dayjs"
import { useQuery } from "react-query"

import "./App.scss"

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
  const { data, isLoading } = useQuery("transaction-summary", () => getTransactionSummary({
    from: dayjs().subtract(1, "month").toDate(),
    to: dayjs().toDate(),
  }))

  console.log("data", data)
  return (
    <ChakraProvider theme={theme}>
      <Box textAlign="center" fontSize="xl">
        <Grid minH="100vh">
          <Center>
            <VStack>
              {isLoading ?
                <Spinner /> :
                <HStack spacing={10}>
                  <Stat>
                    <StatLabel>In</StatLabel>
                    <StatNumber>${data.in}</StatNumber>
                  </Stat>
                  <Stat>
                    <StatLabel>Out</StatLabel>
                    <StatNumber>${data.out}</StatNumber>
                  </Stat>
                </HStack>}
            </VStack>
          </Center>
        </Grid>
      </Box>
    </ ChakraProvider>
  )
}

