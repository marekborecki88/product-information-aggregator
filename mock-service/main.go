package main

import (
	"flag"
	"net/http"
	"time"

	"github.com/gin-gonic/gin"
)

func main() {
	// Define command-line flags
	path := flag.String("path", "/mock", "The endpoint path")
	body := flag.String("body", `{"status": "ok"}`, "The JSON response body")
	delay := flag.Int("delay", 0, "Response delay in milliseconds")
	flag.Parse()

	r := gin.Default()

	// Register the dynamic endpoint
	r.GET(*path, func(c *gin.Context) {
		// Apply artificial delay if specified
		if *delay > 0 {
			time.Sleep(time.Duration(*delay) * time.Millisecond)
		}

		// Return the hardcoded body as application/json
		c.Data(http.StatusOK, "application/json", []byte(*body))
	})

	r.Run(":8080")
}
