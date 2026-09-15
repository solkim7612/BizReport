package com.bizreport.core.entity.history;

import com.bizreport.core.entity.global.BaseEntity;
import com.bizreport.core.entity.user.Users;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import jakarta.persistence.Id;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "REFRESH_HISTORY")
public class RefreshHistory extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "b_id")
    private Users user;

    //todo: enum type 으로 변경
    private String type; // "CHARGE" 또는 "USE"
    private int amount;
    private int balance;

    @Builder
    public RefreshHistory(Users user, String type, int amount, int balance) {
        this.user = user;
        this.type = type;
        this.amount = amount;
        this.balance = balance;
    }
}
