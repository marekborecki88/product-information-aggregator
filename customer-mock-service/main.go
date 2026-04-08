package main

import (
	"net/http"

	"github.com/gin-gonic/gin"
)

type CustomerSegment string

const (
	SegmentDefault CustomerSegment = "DEFAULT"
	SegmentRegular CustomerSegment = "REGULAR"
	SegmentPremium CustomerSegment = "PREMIUM"
	SegmentB2B     CustomerSegment = "B2B"
)

type Preference struct {
	Type  string `json:"type"`
	Value string `json:"value"`
}

type CustomerContext struct {
	Segment     CustomerSegment `json:"segment"`
	Preferences []Preference    `json:"preferences,omitempty"`
}

var customerContexts = map[string]CustomerContext{
	"1": {
		Segment: SegmentRegular,
		Preferences: []Preference{
			{Type: "PREFERRED_SIZE", Value: "M"},
			{Type: "PREFERRED_COLOR", Value: "Blue"},
		},
	},
	"2": {
		Segment: SegmentPremium,
		Preferences: []Preference{
			{Type: "PREFERRED_SIZE", Value: "L"},
			{Type: "DELIVERY_SPEED", Value: "FAST"},
		},
	},
}

const defaultSegment = SegmentDefault

func main() {
	router := gin.Default()

	router.GET("/customer-context/:customerId", getCustomerContext)

	router.Run(":8080")
}

func getCustomerContext(c *gin.Context) {
	customerID := c.Param("customerId")

	// customerID is required
	if customerID == "" {
		c.JSON(http.StatusBadRequest, gin.H{
			"error": "Customer ID is required",
		})
		return
	}

	customerContext, exists := customerContexts[customerID]
	if !exists {
		c.JSON(http.StatusNotFound, gin.H{
			"error": "Customer " + customerID + " not found",
		})
		return
	}

	c.JSON(http.StatusOK, customerContext)
}
