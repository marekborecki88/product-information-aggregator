package main

import (
	"math"
	"net/http"

	"github.com/gin-gonic/gin"
)

type Money struct {
	Amount   float64 `json:"amount"`
	Currency string  `json:"currency"`
}

// ApplyDiscount applies a discount percentage and returns a new Money with rounded amount
func (m Money) ApplyDiscount(discountPercent uint8) Money {
	factor := float64(100-discountPercent) / 100
	roundedAmount := math.Round(m.Amount*factor*100) / 100
	return Money{
		Amount:   roundedAmount,
		Currency: m.Currency,
	}
}

type PricePayload struct {
	BasePrice        Money `json:"basePrice"`
	CustomerDiscount uint8 `json:"customerDiscount"`
	FinalPrice       Money `json:"finalPrice"`
}

func main() {
	router := gin.Default()

	// Public endpoint for getting price by product ID
	router.GET("/prices/:id", getPrice)

	router.Run(":8080")
}

func getPrice(c *gin.Context) {
	productID := c.Param("id")
	market := c.Query("market")
	customerId := c.Query("customerId")

	if productID == "" {
		c.JSON(http.StatusBadRequest, gin.H{"error": "Product ID is required"})
		return
	}

	if market == "" {
		c.JSON(http.StatusBadRequest, gin.H{"error": "Market parameter is required"})
		return
	}

	// Only return price for product ID 3
	if productID != "3" {
		c.JSON(http.StatusNotFound, gin.H{"error": "Price not found for product ID: " + productID})
		return
	}

	// Get base price by market
	basePrice := getBasePriceByMarket(market)
	if basePrice == nil {
		c.JSON(http.StatusBadRequest, gin.H{"error": "Unsupported market: " + market})
		return
	}

	// Calculate discount based on customerId
	var discount uint8
	if customerId == "" {
		discount = 0
	} else {
		discount = getDiscountForCustomer(customerId)
	}

	// Calculate final price with discount applied and rounded
	finalPrice := basePrice.ApplyDiscount(discount)

	price := PricePayload{
		BasePrice:        *basePrice,
		CustomerDiscount: discount,
		FinalPrice:       finalPrice,
	}

	c.JSON(http.StatusOK, price)
}

func getBasePriceByMarket(market string) *Money {
	// Base prices by market (hardcoded map)
	basePrices := map[string]Money{
		"de-DE": {Amount: 179.99, Currency: "EUR"},
		"nl-NL": {Amount: 185.50, Currency: "EUR"},
		"pl-PL": {Amount: 799.99, Currency: "PLN"},
	}

	if price, exists := basePrices[market]; exists {
		return &price
	}
	return nil
}

func getDiscountForCustomer(customerId string) uint8 {
	// Discounts by customer ID
	discounts := map[string]uint8{
		"1": 10, // 10% discount
		"2": 15, // 15% discount
		"3": 20, // 20% discount
	}

	if discount, exists := discounts[customerId]; exists {
		return discount
	}
	return 0 // Default: no discount for unknown customers
}
