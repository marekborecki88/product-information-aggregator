package main

import (
	"net/http"
	"time"

	"github.com/gin-gonic/gin"
)

type ProductAvailability struct {
	StockLevel        int    `json:"stockLevel"`
	WarehouseLocation string `json:"warehouseLocation"`
	ExpectedDelivery  string `json:"expectedDelivery"`
}

// Mock availability data by market
var availabilityByMarket = map[string]ProductAvailability{
	"pl-PL": {
		StockLevel:        25,
		WarehouseLocation: "Konin",
		ExpectedDelivery:  time.Now().Format("2006-01-02"),
	},
	"de-DE": {
		StockLevel:        15,
		WarehouseLocation: "Berlin",
		ExpectedDelivery:  time.Now().AddDate(0, 0, 1).Format("2006-01-02"),
	},
	"nl-NL": {
		StockLevel:        8,
		WarehouseLocation: "Amsterdam",
		ExpectedDelivery:  time.Now().AddDate(0, 0, 2).Format("2006-01-02"),
	},
}

func main() {
	router := gin.Default()

	// Public endpoint for getting availability by product ID
	router.GET("/availability/:id", getAvailability)

	router.Run(":8080")
}

func getAvailability(c *gin.Context) {
	productID := c.Param("id")
	market := c.Query("market")

	if productID == "" {
		c.JSON(http.StatusBadRequest, gin.H{"error": "Product ID is required"})
		return
	}

	if market == "" {
		c.JSON(http.StatusBadRequest, gin.H{"error": "Market parameter is required"})
		return
	}

	// Only return availability for product ID 3
	if productID != "3" {
		c.JSON(http.StatusNotFound, gin.H{"error": "Availability not found for product ID: " + productID})
		return
	}

	// Get availability by market
	availability, exists := availabilityByMarket[market]
	if !exists {
		c.JSON(http.StatusBadRequest, gin.H{"error": "Unsupported market: " + market})
		return
	}

	c.JSON(http.StatusOK, availability)
}
