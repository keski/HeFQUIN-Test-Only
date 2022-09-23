package se.liu.ida.hefquin.engine.wrappers.lpgwrapper.data.impl;

import se.liu.ida.hefquin.engine.wrappers.lpgwrapper.data.Value;

import java.util.Map;

public class LPGNodeValue implements Value {

    protected final LPGNode node;

    public LPGNodeValue(final LPGNode node) {
        this.node = node;
    }

    public LPGNode getNode() {
        return node;
    }

    @Override
    public String toString() {
        return node.toString();
    }
}
