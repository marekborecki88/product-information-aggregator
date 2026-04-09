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

var delayMs int = 100
var errorStatus int = 0

func main() {
	router := gin.Default()

	// Public endpoint for getting availability by product ID
	router.GET("/availability/:id", getAvailability)

	// Admin endpoint for setting delay
	router.POST("/admin/delay", setDelay)

	// Admin endpoint for setting error
	router.POST("/admin/error", setError)

	router.Run(":8080")
}

func setError(c *gin.Context) {
	var body struct {
		Error int `json:"error"`
	}

	if err := c.BindJSON(&body); err != nil {
		c.JSON(http.StatusBadRequest, gin.H{"error": "Invalid request body"})
		return
	}

	// Reset if empty object or error is 0
	if body.Error == 0 {
		errorStatus = 0
		c.JSON(http.StatusOK, gin.H{"message": "Error status reset", "errorStatus": errorStatus})
		return
	}

	errorStatus = body.Error
	c.JSON(http.StatusOK, gin.H{"message": "Error status set", "errorStatus": errorStatus})
}

func setDelay(c *gin.Context) {
	var body struct {
		DelayMs int `json:"delayMs"`
	}

	if err := c.BindJSON(&body); err != nil {
		c.JSON(http.StatusBadRequest, gin.H{"error": "Invalid request body"})
		return
	}

	delayMs = body.DelayMs
	c.JSON(http.StatusOK, gin.H{"message": "Delay set", "delayMs": delayMs})
}

func getAvailability(c *gin.Context) {
	// Check if error status is set
	if errorStatus > 0 {
		c.JSON(errorStatus, gin.H{"error": "Service error"})
		return
	}

	// Apply delay if set
	if delayMs > 0 {
		time.Sleep(time.Duration(delayMs) * time.Millisecond)
	}

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
