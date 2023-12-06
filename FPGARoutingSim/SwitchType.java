public enum SwitchType {
    // switch type of switches in Routing Resource Graph
    mux,            // an isolating, configurable multiplexer
    tristate,       // an isolating, configurable tristate-able buffer
    pass_gate,      // a non-isolating, configurable pass gate
    buffer,         // a non-isolating, non-configurable electrical short
    shorts          // an isolating, non-congigurable non-tristate-able buffer
}
