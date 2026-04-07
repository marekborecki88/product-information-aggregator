package main

import (
	"net/http"

	"github.com/gin-gonic/gin"
)

type Product struct {
	Name        string            `json:"name"`
	Description string            `json:"description"`
	Specs       map[string]string `json:"specs"`
	Images      []string          `json:"images"`
}

func main() {
	router := gin.Default()

	// Public endpoint for getting product by ID and market
	router.GET("/products/:id", getProduct)

	router.Run(":8080")
}

func getProduct(c *gin.Context) {
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
			"https://images.example.com/products/" + productID + "/main.jpg",
			"https://images.example.com/products/" + productID + "/tractor-wheel.jpg",
			"https://images.example.com/products/" + productID + "/tractor-engine.jpg",
			"https://images.example.com/products/" + productID + "/tractor-cabin.jpg",
		},
	}

	c.JSON(http.StatusOK, product)
}
