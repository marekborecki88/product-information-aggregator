package main

import (
	"net/http"

	"github.com/gin-gonic/gin"
)

type Money struct {
	Currency string  `json:"currency"`
	Amount   float64 `json:"amount"`
}

type Product struct {
	ID          string   `json:"id"`
	Name        string   `json:"name"`
	Price       Money    `json:"price"`
	Market      string   `json:"market"`
	Description string   `json:"description"`
	Images      []string `json:"images"`
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

	// Mock product data
	product := Product{
		ID:          productID,
		Name:        "Sample Product " + productID,
		Price:       Money{Currency: "PLN", Amount: 98.99},
		Market:      market,
		Description: "This is a mock product for market " + market,
		Images: []string{
			"https://images.example.com/products/" + productID + "/main.jpg",
			"https://images.example.com/products/" + productID + "/tractor-wheel.jpg",
			"https://images.example.com/products/" + productID + "/tractor-engine.jpg",
			"https://images.example.com/products/" + productID + "/tractor-cabin.jpg",
		},
	}

	c.JSON(http.StatusOK, product)
}
