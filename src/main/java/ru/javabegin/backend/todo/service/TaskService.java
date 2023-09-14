package ru.javabegin.backend.todo.service;

import javassist.NotFoundException;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import ru.javabegin.backend.todo.aop.exception.IncorrectDataException;
import ru.javabegin.backend.todo.entity.Task;
import ru.javabegin.backend.todo.repo.TaskRepository;
import ru.javabegin.backend.todo.search.TaskSearchValues;

import javax.transaction.Transactional;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import static ru.javabegin.backend.todo.controller.TaskController.ID_COLUMN;

// всегда нужно создавать отдельный класс Service для доступа к данным, даже если кажется,
// что мало методов или это все можно реализовать сразу в контроллере
// Такой подход полезен для будущих доработок и правильной архитектуры (особенно, если работаете с транзакциями)
@Service

// все методы класса должны выполниться без ошибки, чтобы транзакция завершилась
// если в методе возникнет исключение - все выполненные операции откатятся (Rollback)
@Transactional
public class TaskService {

    private final TaskRepository repository; // сервис имеет право обращаться к репозиторию (БД)

    public TaskService(TaskRepository repository) {
        this.repository = repository;
    }

    @Cacheable(cacheNames = "tasks")
    public List<Task> findAll(String email) {
        return repository.findByUserEmailOrderByTitleAsc(email);
    }

    public Task add(Task task) {
        return repository.save(task); // метод save обновляет или создает новый объект, если его не было
    }

    public Task update(Task task) {
        return repository.save(task); // метод save обновляет или создает новый объект, если его не было
    }

    public void deleteById(Long id) {
        repository.deleteById(id);
    }

    @Cacheable(cacheNames = "tasks")
    public Page<Task> findByParams(TaskSearchValues taskSearchValues) {
        // исключить NullPointerException
        String title = taskSearchValues.getTitle() != null ? taskSearchValues.getTitle() : null;

        // конвертируем Boolean в Integer
        Boolean completed = taskSearchValues.getCompleted() != null && taskSearchValues.getCompleted() == 1 ? true : false;

        Long priorityId = taskSearchValues.getPriorityId() != null ? taskSearchValues.getPriorityId() : null;
        Long categoryId = taskSearchValues.getCategoryId() != null ? taskSearchValues.getCategoryId() : null;

        String sortColumn = taskSearchValues.getSortColumn() != null ? taskSearchValues.getSortColumn() : null;
        String sortDirection = taskSearchValues.getSortDirection() != null ? taskSearchValues.getSortDirection() : null;

        Integer pageNumber = taskSearchValues.getPageNumber() != null ? taskSearchValues.getPageNumber() : null;
        Integer pageSize = taskSearchValues.getPageSize() != null ? taskSearchValues.getPageSize() : null;


        // чтобы захватить в выборке все задачи по датам, независимо от времени - можно выставить время с 00:00 до 23:59

        Date dateFrom = null;
        Date dateTo = null;


        // выставить 00:01 для начальной даты (если она указана)
        if (taskSearchValues.getDateFrom() != null) {
            Calendar calendarFrom = Calendar.getInstance();
            calendarFrom.setTime(taskSearchValues.getDateFrom());
            calendarFrom.set(Calendar.HOUR_OF_DAY, 0);
            calendarFrom.set(Calendar.MINUTE, 1);
            calendarFrom.set(Calendar.SECOND, 1);
            calendarFrom.set(Calendar.MILLISECOND, 1);

            dateFrom = calendarFrom.getTime(); // записываем начальную дату с 00:01

        }
        String email = taskSearchValues.getEmail() != null ? taskSearchValues.getEmail() : null; // для показа задач только этого пользователя

        // проверка на обязательные параметры
        if (email == null || email.trim().length() == 0) {
            throw new IncorrectDataException("missed param: email" + email);
        }


        // выставить 23:59 для конечной даты (если она указана)
        if (taskSearchValues.getDateTo() != null) {

            Calendar calendarTo = Calendar.getInstance();
            calendarTo.setTime(taskSearchValues.getDateTo());
            calendarTo.set(Calendar.HOUR_OF_DAY, 23);
            calendarTo.set(Calendar.MINUTE, 59);
            calendarTo.set(Calendar.SECOND, 59);
            calendarTo.set(Calendar.MILLISECOND, 999);

            dateTo = calendarTo.getTime(); // записываем конечную дату с 23:59

        }


        // направление сортировки
        Sort.Direction direction = sortDirection == null || sortDirection.trim().length() == 0 || sortDirection.trim().equals("asc") ? Sort.Direction.ASC : Sort.Direction.DESC;

        /* Вторым полем для сортировки добавляем id, чтобы всегда сохранялся строгий порядок.
            Например, если у 2-х задач одинаковое значение приоритета и мы сортируем по этому полю.
            Порядок следования этих 2-х записей после выполнения запроса может каждый раз меняться, т.к. не указано второе поле сортировки.
            Поэтому и используем ID - тогда все записи с одинаковым значением приоритета будут следовать в одном порядке по ID.
         */

        // объект сортировки, который содержит стобец и направление
        Sort sort = Sort.by(direction, sortColumn, ID_COLUMN);

        // объект постраничности
        PageRequest pageRequest = PageRequest.of(pageNumber, pageSize, sort);

        return repository.findByParams(title, completed, priorityId, categoryId, email, dateFrom, dateTo, pageRequest);
    }

    public Task findById(Long id) {
        return repository.findById(id).get(); // т.к. возвращается Optional - можно получить объект методом get()
    }


}
