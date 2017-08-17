package pingis.services;

import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import pingis.entities.Challenge;
import pingis.entities.CodeStatus;
import pingis.entities.Task;
import pingis.entities.TaskInstance;
import pingis.entities.TaskType;
import pingis.entities.User;
import pingis.repositories.ChallengeRepository;

@Service
public class ChallengeService {

  private final ChallengeRepository challengeRepository;

  @Autowired
  private TaskService taskService;

  @Autowired
  private UserService userService;

  @Autowired
  private TaskInstanceService taskInstanceService;

  @Autowired
  public ChallengeService(ChallengeRepository challengeRepo) {
    this.challengeRepository = challengeRepo;
  }

  public Challenge findOne(Long challengeId) {
    // Implement validation here
    return challengeRepository.findOne(challengeId);
  }

  public Challenge save(Challenge newChallenge) {
    // Implement validation here
    return challengeRepository.save(newChallenge);
  }

  public List<Challenge> findAll() {
    return (List) challengeRepository.findAll();
  }

  public Challenge delete(Long challengeId) {
    //Implement validation here
    Challenge c = findOne(challengeId);
    challengeRepository.delete(challengeId);
    return c;
  }

  public boolean contains(Long challengeId) {
    return challengeRepository.exists(challengeId);
  }

  public Challenge findByName(String name) {
    return challengeRepository.findByName(name);
  }

  public int getNumberOfTasks(Challenge challenge) {
    List<Task> tasks = taskService.findAllByChallenge(challenge);
    return tasks.size();
  }

  public TaskInstance getUnfinishedTaskInstance(Challenge challenge) {
    User user = userService.getCurrentUser();
    List<TaskInstance> taskInstances = taskInstanceService.getAllByChallenge(challenge);
    for (TaskInstance current : taskInstances) {
      if (current.getStatus() == CodeStatus.IN_PROGRESS) {
        return current;
      }
    }
    return null;
  }

  public boolean isTestTurnInLiveChallenge(Challenge challenge) {
    User user = userService.getCurrentUser();
    int side = user.getId() == challenge.getAuthor().getId() ? 0 : 1;
    int numberOfTasks = getNumberOfTasks(challenge);
    int highestIndex = numberOfTasks / 2;
    int numberOfDoneInstances =
        taskInstanceService.getNumberOfDoneTaskInstancesInChallenge(challenge);
    if (highestIndex == 1) {
      if (numberOfDoneInstances == 0 && user.getId() == challenge.getAuthor().getId()) {
        return true;
      } else if (numberOfDoneInstances == 2 && user.getId() == challenge.getSecondPlayer().getId()) {
        return true;
      }
      return false;
    }
    if (numberOfDoneInstances % 2 == 0 && highestIndex % 2 == side) {
      return true;
    }
    return false;
  }
  
  public boolean isImplementationTurnInLiveChallenge(Challenge challenge) {
    User user = userService.getCurrentUser();
    int side = user.getId() == challenge.getAuthor().getId() ? 0 : 1;
    int numberOfTasks = getNumberOfTasks(challenge);
    int highestIndex = numberOfTasks / 2;
    int numberOfDoneInstances =
        taskInstanceService.getNumberOfDoneTaskInstancesInChallenge(challenge);
    if (numberOfDoneInstances % 2 == 1 && highestIndex % 2 == side) {
      return true;
    }
    return false;
  }

  public boolean isParticipating(Challenge challenge) {
    User currentUser = userService.getCurrentUser();
    if (currentUser.getId() == challenge.getAuthor().getId()) {
      return true;
    }

    if (challenge.getSecondPlayer() == null) {
      return false;
    }

    return (currentUser.getId() == challenge.getSecondPlayer().getId());
  }

  public Task getTopmostImplementationTask(Challenge challenge, int index) {
    List<Task> tasks = taskService.findAllByChallenge(challenge);
    for (Task task : tasks) {
      if (task.getIndex() == index && task.getType() == TaskType.IMPLEMENTATION) {
        return task;
      }
    }
    return null;
  }

  public Task getTopmostTestTask(Challenge challenge, int index) {
    List<Task> tasks = taskService.findAllByChallenge(challenge);
    for (Task task : tasks) {
      if (task.getIndex() == index && task.getType() == TaskType.TEST) {
        return task;
      }
    }
    return null;
  }
  
  public Challenge getRandomLiveChallenge(User user) {
    List<Challenge> liveChallenges = findAll().stream()
            .filter(e -> e.getIsOpen())
            .filter(e -> e.getSecondPlayer() == null)
            .filter(e -> e.getAuthor() != user)
            .collect(Collectors.toList());
   
    if (liveChallenges.isEmpty()) {
      return null;
    }
    if (liveChallenges.size() > 1) {
      return liveChallenges.get(new Random().nextInt(liveChallenges.size()));
    }
    return liveChallenges.get(0);
  }

  public Challenge getParticipatingLiveChallenge() {
    Optional<Challenge> findFirst = findAll().stream()
            .filter(e -> e.getIsOpen())
            .filter(e -> isParticipating(e))
            .findFirst();
    
    if (findFirst.isPresent()) {
      return findFirst.get();
    } else {
      return null;
    }
  }


}
