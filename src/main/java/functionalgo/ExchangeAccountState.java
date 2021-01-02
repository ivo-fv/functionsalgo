package functionalgo;

/**
 * Implementations of ExchangeAccountState should store the necessary information to describe an exchange
 * account's state such as PnL of each position, margin balance and wallet balance, exactly as it would appear
 * on the live exchange API.
 */
public interface ExchangeAccountState {
    /* TODO consider making more specific interfaces that extend this one such as a specific perpetual one in functionalgo.perpetual: PerpetualExchangeAccountState */
}
