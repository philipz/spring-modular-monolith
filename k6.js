// k6 load test for Spring Modular Monolith Orders API
// Tests the complete order creation flow: add to cart -> create order

import { sleep, group, check } from 'k6'
import http from 'k6/http'

export const options = {
  vus: 10, // 10 virtual users
  duration: '30s', // run for 30 seconds
}

export default function main() {
  const baseUrl = 'http://localhost:8080'
  const productCodes = ['P100', 'P101', 'P102', 'P103', 'P104']

  // Select random product code for this iteration
  const productCode = productCodes[Math.floor(Math.random() * productCodes.length)]

  group('Complete Order Flow', function () {
    // Step 1: Add product to cart
    const buyResponse = http.post(
      `${baseUrl}/buy?code=${productCode}`,
      null,
      {
        headers: {
          'Content-Type': 'application/x-www-form-urlencoded',
          'Accept': 'text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8',
        },
        redirects: 0, // Don't follow redirects automatically to capture session
      }
    )

    check(buyResponse, {
      'add to cart redirects to /cart': (r) => r.status === 302 && r.headers['Location'].includes('/cart'),
    })

    // Extract session cookie
    const cookies = buyResponse.cookies
    const sessionCookie = cookies['SESSION']

    if (!sessionCookie) {
      console.error('No session cookie found after adding to cart')
      return
    }

    // Step 2: Create order with customer details
    const orderPayload = {
      'customer.name': 'K6 Test User',
      'customer.email': `k6test${__VU}@example.com`,
      'customer.phone': '+1-555-0100',
      'deliveryAddress': `${__VU} Test Street, K6 City`,
    }

    const orderResponse = http.post(
      `${baseUrl}/orders`,
      orderPayload,
      {
        headers: {
          'Content-Type': 'application/x-www-form-urlencoded',
          'Accept': 'text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8',
          'Cookie': `SESSION=${sessionCookie.value}`,
        },
        redirects: 0, // Don't follow redirects to check order number
      }
    )

    check(orderResponse, {
      'order created successfully': (r) => r.status === 302,
      'redirects to order details': (r) => r.headers['Location'] && r.headers['Location'].includes('/orders/'),
    })

    if (orderResponse.status === 302 && orderResponse.headers['Location']) {
      const orderNumber = orderResponse.headers['Location'].split('/').pop()
      console.log(`Order created: ${orderNumber} with product ${productCode}`)
    }
  })

  sleep(1)
}
