package com.kodari.housing.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CommittedBid {
    private String id;
    private UUID bidder;
    private double diamonds;
    private double balance;
    private boolean diamondsHeld;
    private boolean balanceHeld;
    private String status;
}