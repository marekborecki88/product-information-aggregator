package main

import (
	"net/http"
	"time"

	"github.com/gin-gonic/gin"
)

type Product struct {
	Name        string            `json:"name"`
	Description string            `json:"description"`
	Specs       map[string]string `json:"specs"`
	Images      []string          `json:"images"`
}

var delayMs int = 0

func main() {
	router := gin.Default()

	// Public endpoint for getting product by ID and market
	router.GET("/catalog/products/:id", getProduct)

	// Admin endpoint for setting delay
	router.POST("/admin/delay", setDelay)

	router.Run(":8080")
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

func getProduct(c *gin.Context) {
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

	specs := map[string]string{
		"Color": "Red",
		"Size":  "Medium",
	}

	// Mock product data
	product := Product{
		Name:        "Sample Product " + productID,
		Description: "This is a mock product for market " + market,
		Specs:       specs,
		Images: []string{
			"https://images.example.com/catalog/products/" + productID + "/main.jpg",
			"https://images.example.com/catalog/products/" + productID + "/tractor-wheel.jpg",
			"https://images.example.com/catalog/products/" + productID + "/tractor-engine.jpg",
			"https://images.example.com/catalog/products/" + productID + "/tractor-cabin.jpg",
		},
	}

	c.JSON(http.StatusOK, product)
}
