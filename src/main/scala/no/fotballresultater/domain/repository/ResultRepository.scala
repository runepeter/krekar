package no.fotballresultater.domain.repository

import no.fotballresultater.domain.Result

trait ResultRepository {

  def save(result: Result)

}