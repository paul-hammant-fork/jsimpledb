
/*
 * Copyright (C) 2014 Archie L. Cobbs. All rights reserved.
 *
 * $Id$
 */

package org.jsimpledb.cli;

import java.util.Map;

import org.jsimpledb.util.ParseContext;

public class PopCommand extends Command {

    public PopCommand() {
        super("pop depth:int?");
    }

    @Override
    public String getHelpSummary() {
        return "pops one or more channels off the top of the channel stack";
    }

    @Override
    public String getHelpDetail() {
        return "The `pop' command pops one or more channels off the top of the channel stack. By default only the top"
          + " channel is popped; specify a different depth to pop more than one channel.";
    }

    @Override
    public Action getAction(Session session, ParseContext ctx, boolean complete, Map<String, Object> params) {

        // Parse parameters
        final int depth = params.containsKey("depth") ? (Integer)params.get("depth") : 1;
        if (depth < 0)
            throw new ParseException(ctx, "invalid negative depth");

        // Return action
        return new Action() {
            @Override
            public void run(Session session) throws Exception {
                PopCommand.this.checkDepth(session, depth);
                for (int i = 0; i < depth; i++)
                    PopCommand.this.pop(session);
            }
        };
    }
}

