// k6 load test for Spring Modular Monolith Orders API
// Tests the complete order creation flow: add to cart -> create order

import { sleep, group, check } from 'k6'
import http from 'k6/http'

export const options = {
  vus: 10, // 10 virtual users
  duration: '30s', // run for 30 seconds
}

export default function main() {
  const baseUrl = __ENV.BASE_URL || 'http://localhost'
  const productCodes = ['P100', 'P101', 'P102', 'P103', 'P104']

  // Select random product code for this iteration
  const productCode = productCodes[Math.floor(Math.random() * productCodes.length)]

  group('Complete Order Flow', function () {
    // Step 1: Add product to cart via REST API
    const addToCartPayload = JSON.stringify({ code: productCode, quantity: 1 })

    const cartResponse = http.post(`${baseUrl}/api/cart/items`, addToCartPayload, {
      headers: {
        'Content-Type': 'application/json',
        Accept: 'application/json',
      },
    })

    check(cartResponse, {
      'item added to cart': (r) => r.status === 201,
    })

    const sessionCookie = cartResponse.cookies['BOOKSTORE_SESSION']?.[0]

    if (!sessionCookie) {
      console.error('No BOOKSTORE_SESSION cookie found after adding to cart')
      return
    }

    const cartBody = cartResponse.json()
    const cartItem = cartBody?.items && cartBody.items.length > 0 ? cartBody.items[0] : null

    if (!cartItem) {
      console.error('Cart response did not include any items')
      return
    }

    // Step 2: Create order with customer details
    const orderPayload = JSON.stringify({
      customer: {
        name: 'K6 Test User',
        email: `k6test${__VU}@example.com`,
        phone: '+1-555-0100',
      },
      deliveryAddress: `${__VU} Test Street, K6 City`,
      item: {
        code: cartItem.code,
        name: cartItem.name,
        price: cartItem.price,
        quantity: cartItem.quantity,
      },
    })

    const orderResponse = http.post(`${baseUrl}/api/orders`, orderPayload, {
      headers: {
        'Content-Type': 'application/json',
        Accept: 'application/json',
        Cookie: `BOOKSTORE_SESSION=${sessionCookie.value}`,
      },
    })

    check(orderResponse, {
      'order created successfully': (r) => r.status === 201,
    })

    if (orderResponse.status === 201) {
      const responseBody = orderResponse.json()
      const orderNumber =
        responseBody?.orderNumber ||
        (orderResponse.headers['Location'] && orderResponse.headers['Location'].split('/').pop())

      if (orderNumber) {
        console.log(`Order created: ${orderNumber} with product ${productCode}`)
      } else {
        console.warn('Order created but order number was not returned in body or Location header')
      }
    }
  })

  sleep(1)
}
