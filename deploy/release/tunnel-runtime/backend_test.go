package main

import (
	"encoding/json"
	"testing"
	"time"
)

func TestParseAPITimeAcceptsJacksonMillisecondsAndISO(t *testing.T) {
	now := time.Now().UTC().Truncate(time.Millisecond)
	milliseconds, _ := json.Marshal(now.UnixMilli())
	parsedMilliseconds, err := parseAPITime(milliseconds)
	if err != nil || !parsedMilliseconds.Equal(now) {
		t.Fatalf("milliseconds parse failed: %v %v", parsedMilliseconds, err)
	}
	iso, _ := json.Marshal(now.Format(time.RFC3339Nano))
	parsedISO, err := parseAPITime(iso)
	if err != nil || !parsedISO.Equal(now) {
		t.Fatalf("ISO parse failed: %v %v", parsedISO, err)
	}
}
