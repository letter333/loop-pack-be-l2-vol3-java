package com.loopers.infrastructure.eventtracker;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class AggregateEventTrackerId implements Serializable {

    private String aggregateId;
    private String eventType;
}
