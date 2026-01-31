package com.example.hierarchicalbots.planning;

import com.example.hierarchicalbots.perception.PerceptionSnapshot;
import com.example.hierarchicalbots.social.SocialMessage;
import java.util.List;

public interface HighLevelPlanner {
    GoalIntent plan(PerceptionSnapshot snapshot, List<SocialMessage> messages);
}
