package org.do6po.cicero.configuration;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Collection;
import java.util.function.Function;
import java.util.stream.Collectors;
import javax.sql.DataSource;
import org.do6po.cicero.exception.BaseException;
import org.do6po.cicero.expression.Expression;
import org.do6po.cicero.interceptor.ConnectionInterceptor;
import org.do6po.cicero.query.grammar.Grammar;

public interface DbDriver {
  DataSource getDataSource();

  Connection getConnection();

  ConnectionInterceptor getInterceptor();

  Grammar getGrammar();

  default int executeWriteQuery(Function<DbDriver, Expression> function) {
    return executeWriteQuery(function.apply(this));
  }

  default int executeWriteQuery(Expression expression) {
    String sqlExpression = expression.getExpression();
    Collection<Object> bindings = expression.getBindings();
    String bindingAsString =
        bindings.stream().map(String::valueOf).collect(Collectors.joining(", "));

    try {
      PreparedStatement preparedStatement = getConnection().prepareStatement(sqlExpression);

      int i = 0;

      for (Object binding : bindings) {
        preparedStatement.setObject(++i, binding);
      }

      return preparedStatement.executeUpdate();
    } catch (SQLException e) {
      String message =
          """
          Builder.executeQuery error:
          %s
          Sql state: '%s'.
          Query: '%s'.
          Bindings: (%s).
          """
              .formatted(e.getMessage(), e.getSQLState(), sqlExpression, bindingAsString);
      throw new BaseException(message, e);
    }
  }
}
